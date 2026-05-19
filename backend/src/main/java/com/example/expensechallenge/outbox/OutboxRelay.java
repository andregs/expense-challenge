package com.example.expensechallenge.outbox;

import com.example.expensechallenge.domain.OutboxEvent;
import com.example.expensechallenge.infrastructure.messaging.TransactionEventPublisher;
import com.example.expensechallenge.infrastructure.persistence.OutboxEventRepository;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox_events table for PENDING rows and forwards each one to
 * Kafka. {@code SELECT ... FOR UPDATE SKIP LOCKED} prevents concurrent relay
 * instances (e.g. multiple pods) from picking up the same event.
 *
 * <p>Each event is published synchronously and immediately marked PUBLISHED
 * within the same DB transaction. A failure on any individual event is caught
 * and logged; {@code retry_count} is incremented and the event stays PENDING
 * so the next cycle retries it. Once {@code retry_count} reaches
 * {@code outbox.relay.max-retries} the event is promoted to FAILED (dead-letter)
 * and excluded from future relay batches. Operators can re-queue a FAILED event
 * by resetting its status and retry_count directly in the database.
 *
 * <p>Ordering guarantee: events are fetched {@code ORDER BY created_at} and
 * published sequentially. Kafka key = {@code aggregateId}, so same-aggregate
 * events always land in the same partition in arrival order. A retry gap
 * (event A fails, event B for the same aggregate succeeds, A is retried next
 * cycle) would cause out-of-order delivery — but only if an aggregate emits
 * multiple sequential events. Today each purchase produces exactly one outbox
 * event, so this cannot occur.
 *
 * <p>Set {@code outbox.relay.enabled=false} to disable this component
 * entirely (default in the {@code test} Spring profile).
 */
@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxRepository;
    private final TransactionEventPublisher publisher;
    private final int batchSize;
    private final int maxRetries;

    public OutboxRelay(
        OutboxEventRepository outboxRepository,
        TransactionEventPublisher publisher,
        @Value("${outbox.relay.batch-size:50}") int batchSize,
        @Value("${outbox.relay.max-retries:5}") int maxRetries
    ) {
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:2000}")
    @Transactional
    public void relay() {
        var batch = outboxRepository.findPendingBatch(batchSize);
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Relaying {} outbox event(s)", batch.size());
        OffsetDateTime now = OffsetDateTime.now();

        for (var event : batch) {
            try {
                publisher.publish(event).get(10, TimeUnit.SECONDS);
                outboxRepository.save(event.markPublished(now));
            } catch (Exception e) {
                handleFailure(event, e);
            }
        }
    }

    private void handleFailure(OutboxEvent event, Exception cause) {
        int attempts = event.retryCount() + 1;
        if (attempts >= maxRetries) {
            log.error("Outbox event {} (type={}) exhausted {} retries, marking FAILED: {}",
                event.id(), event.type(), maxRetries, cause.getMessage());
            outboxRepository.save(event.markFailed());
        } else {
            log.warn("Outbox event {} (type={}) failed (attempt {}/{}): {}",
                event.id(), event.type(), attempts, maxRetries, cause.getMessage());
            outboxRepository.save(event.incrementRetry());
        }
    }
}
