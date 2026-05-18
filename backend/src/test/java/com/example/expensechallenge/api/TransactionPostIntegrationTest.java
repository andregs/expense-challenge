package com.example.expensechallenge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensechallenge.TestcontainersConfiguration;
import com.example.expensechallenge.domain.OutboxStatus;
import com.example.expensechallenge.infrastructure.persistence.OutboxEventRepository;
import com.example.expensechallenge.infrastructure.persistence.PurchaseTransactionRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full-stack POST /api/v1/transactions integration tests.
 *
 * <p>{@link SpringBootTest} with a {@code MOCK} servlet environment gives us a real application
 * context (Flyway-migrated schema, transaction management, Jackson serialisation) while keeping
 * the HTTP layer in-process via {@link MockMvc}. The {@link Transactional} annotation causes each
 * test method and its MockMvc dispatch to share one transaction that rolls back at the end, so
 * tests are hermetic without any manual cleanup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransactionPostIntegrationTest {

    private static final String URL = "/api/v1/transactions";
    private static final String UUID_PATTERN =
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired MockMvc mockMvc;
    @Autowired PurchaseTransactionRepository transactionRepo;
    @Autowired OutboxEventRepository outboxRepo;

    @Test
    void happyPath_returns201_andPersistsTransactionWithOutboxEvent() throws Exception {
        MvcResult result = mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Office supplies",
                      "transactionDate": "2026-05-01",
                      "purchaseAmountUsd": "125.49"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().string("Location", containsString("/api/v1/transactions/")))
            .andExpect(jsonPath("$.id").value(matchesPattern(UUID_PATTERN)))
            .andExpect(jsonPath("$.description").value("Office supplies"))
            .andExpect(jsonPath("$.transactionDate").value("2026-05-01"))
            .andExpect(jsonPath("$.purchaseAmountUsd").value("125.49"))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(responseBody, "$.id"));

        assertThat(transactionRepo.findById(id)).isPresent();

        var outboxEvents = outboxRepo.findAll();
        assertThat(outboxEvents).hasSize(1);
        var event = outboxEvents.iterator().next();
        assertThat(event.aggregateId()).isEqualTo(id);
        assertThat(event.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.type()).isEqualTo("purchase.transaction.created");
        assertThat(event.payload()).contains("\"id\":\"" + id + "\"");
        assertThat(event.payload()).contains("\"purchaseAmountUsd\":\"125.49\"");
    }

    @Test
    void descriptionTooLong_returns400WithFieldError() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "%s",
                      "transactionDate": "2026-05-01",
                      "purchaseAmountUsd": "10.00"
                    }
                    """.formatted("A".repeat(51))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].field").value("description"));
    }

    @Test
    void blankDescription_returns400WithFieldError() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "   ",
                      "transactionDate": "2026-05-01",
                      "purchaseAmountUsd": "10.00"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("description"));
    }

    @Test
    void zeroPurchaseAmount_returns400WithFieldError() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Test",
                      "transactionDate": "2026-05-01",
                      "purchaseAmountUsd": "0.00"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("purchaseAmountUsd"));
    }

    @Test
    void negativePurchaseAmount_returns400WithFieldError() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Test",
                      "transactionDate": "2026-05-01",
                      "purchaseAmountUsd": "-1.00"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("purchaseAmountUsd"));
    }

    @Test
    void missingTransactionDate_returns400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Test",
                      "purchaseAmountUsd": "10.00"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void malformedDate_returns400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Test",
                      "transactionDate": "not-a-date",
                      "purchaseAmountUsd": "10.00"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void transactionDateAsEpochNumber_returns400() throws Exception {
        // Clients must send ISO-8601 date strings; bare epoch millis are rejected
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Test",
                      "transactionDate": 1746086400,
                      "purchaseAmountUsd": "10.00"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void transactionDateWithTimePart_returns400() throws Exception {
        // The field is date-only; a datetime string must not be silently truncated
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Test",
                      "transactionDate": "2026-05-01T10:30:00",
                      "purchaseAmountUsd": "10.00"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void numericPurchaseAmount_returns400() throws Exception {
        // The spec defines purchaseAmountUsd as a JSON string to preserve precision;
        // a bare JSON number must be rejected rather than silently coerced
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Test",
                      "transactionDate": "2026-05-01",
                      "purchaseAmountUsd": 125.49
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
}
