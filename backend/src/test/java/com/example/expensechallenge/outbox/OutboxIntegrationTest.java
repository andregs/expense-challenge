package com.example.expensechallenge.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensechallenge.TestcontainersConfiguration;
import com.example.expensechallenge.domain.OutboxEvent;
import com.example.expensechallenge.domain.OutboxStatus;
import com.example.expensechallenge.infrastructure.messaging.TransactionEventPublisher;
import com.example.expensechallenge.infrastructure.persistence.OutboxEventRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end test for the transactional outbox relay.
 *
 * <p>The relay bean is enabled for this class only ({@code @SpringBootTest}
 * properties), but the scheduler interval is set to 1 hour so the scheduled
 * timer never fires mid-test — each test invokes {@code outboxRelay.relay()}
 * directly. {@code relay()} is synchronous via the Spring {@code @Transactional}
 * proxy, so by the time the call returns Kafka has acknowledged the write
 * (acks=all) and the DB transaction is committed; only the consumer-side
 * delivery to {@link KafkaListener} still needs a short poll.
 *
 * <p>A nested {@link TestConfiguration} wires a listener that feeds received
 * events into a {@link BlockingQueue}. The queue is cleared in {@code @BeforeEach}
 * so any startup-fire records or events from preceding test classes are
 * discarded before each test runs.
 *
 * <p>{@link MockitoSpyBean} wraps {@link TransactionEventPublisher} so the
 * failure-path test can stub it to return a failed future without affecting
 * the other tests (the spy is reset in {@code @AfterEach}).
 */
@SpringBootTest(properties = {
    "outbox.relay.enabled=true",
    "outbox.relay.fixed-delay=3600000",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.group-id=test-outbox-consumer"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DirtiesContext
class OutboxIntegrationTest {

    @TestConfiguration
    static class TestConsumerConfig {

        static final BlockingQueue<ConsumerRecord<String, String>> received = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "purchase.transactions.created")
        void onRecord(ConsumerRecord<String, String> record) {
            received.add(record);
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired OutboxEventRepository outboxRepository;
    @Autowired OutboxRelay outboxRelay;
    @MockitoSpyBean TransactionEventPublisher publisher;

    @BeforeEach
    void setUp() {
        TestConsumerConfig.received.clear();
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(publisher);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void postedTransaction_createsOutboxRowAsPending() throws Exception {
        UUID txId = postTransaction("Laptop", "2026-05-01", "1500.00");

        OutboxEvent event = findOutboxEvent(txId);
        assertThat(event.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.publishedAt()).isNull();
        assertThat(event.type()).isEqualTo("purchase.transaction.created");
        assertThat(event.payload()).contains("\"id\":\"" + txId + "\"");
    }

    @Test
    void relay_publishesToKafkaAndMarksPublished() throws Exception {
        UUID txId = postTransaction("Laptop", "2026-05-01", "1500.00");

        outboxRelay.relay();

        OutboxEvent event = findOutboxEvent(txId);
        assertThat(event.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.publishedAt()).isNotNull();

        ConsumerRecord<String, String> record = awaitRecord(txId);
        assertThat(record.key()).isEqualTo(txId.toString());
        assertThat(record.value())
            .contains("\"id\":\"" + txId + "\"")
            .contains("\"purchaseAmountUsd\":\"1500.00\"");
    }

    @Test
    void relay_publishesAllPendingEventsInOneBatch() throws Exception {
        UUID id1 = postTransaction("Item 1", "2026-05-01", "10.00");
        UUID id2 = postTransaction("Item 2", "2026-05-01", "20.00");
        UUID id3 = postTransaction("Item 3", "2026-05-01", "30.00");

        outboxRelay.relay();

        for (UUID id : List.of(id1, id2, id3)) {
            OutboxEvent event = findOutboxEvent(id);
            assertThat(event.status()).as("status for %s", id).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(event.publishedAt()).as("publishedAt for %s", id).isNotNull();
        }
    }

    @Test
    void publishFailure_leavesRowPendingAndIncrementsRetryCount() throws Exception {
        doReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")))
            .when(publisher).publish(any());

        UUID txId = postTransaction("Laptop", "2026-05-01", "1500.00");
        outboxRelay.relay();

        OutboxEvent event = findOutboxEvent(txId);
        assertThat(event.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.publishedAt()).isNull();
        assertThat(event.retryCount()).isEqualTo(1);
    }

    @Test
    void publishFailure_afterMaxRetries_marksEventFailed() throws Exception {
        doReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")))
            .when(publisher).publish(any());

        UUID txId = postTransaction("Laptop", "2026-05-01", "1500.00");

        // Default max-retries in tests is 5; relay 5 times to exhaust the budget.
        for (int i = 0; i < 5; i++) {
            outboxRelay.relay();
        }

        OutboxEvent event = findOutboxEvent(txId);
        assertThat(event.status()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.publishedAt()).isNull();
        assertThat(event.retryCount()).isEqualTo(4);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID postTransaction(String description, String date, String amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"%s","transactionDate":"%s","purchaseAmountUsd":"%s"}
                    """.formatted(description, date, amount)))
            .andExpect(status().isCreated())
            .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private OutboxEvent findOutboxEvent(UUID aggregateId) {
        return StreamSupport
            .stream(outboxRepository.findAll().spliterator(), false)
            .filter(e -> aggregateId.equals(e.aggregateId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No outbox event for aggregateId=" + aggregateId));
    }

    private ConsumerRecord<String, String> awaitRecord(UUID expectedKey) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            ConsumerRecord<String, String> r =
                TestConsumerConfig.received.poll(200, TimeUnit.MILLISECONDS);
            if (r != null && expectedKey.toString().equals(r.key())) {
                return r;
            }
        }
        throw new AssertionError("No Kafka record for txId " + expectedKey + " within timeout");
    }
}
