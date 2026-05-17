package com.example.expensechallenge.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.expensechallenge.TestcontainersConfiguration;
import com.example.expensechallenge.domain.OutboxEvent;
import com.example.expensechallenge.domain.OutboxStatus;
import com.example.expensechallenge.domain.PurchaseTransaction;
import com.example.expensechallenge.infrastructure.persistence.OutboxEventRepository;
import com.example.expensechallenge.infrastructure.persistence.PurchaseTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Flyway-managed schema and the Spring Data JDBC mapping for
 * the {@link PurchaseTransaction} and {@link OutboxEvent} aggregates.
 *
 * <p>The {@code @DataJdbcTest} slice is wired to the Testcontainers
 * PostgreSQL instance (instead of an embedded H2/HSQLDB) so the production
 * DDL is exercised end-to-end: identifier generation via
 * {@code gen_random_uuid()}, NUMERIC(19,4) precision and the CHECK
 * constraints all execute exactly as they would in production.
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PersistenceIntegrationTest {

    @Autowired
    private PurchaseTransactionRepository transactions;

    @Autowired
    private OutboxEventRepository outbox;

    @Test
    void persistsPurchaseTransactionAndAssignsIdentifier() {
        PurchaseTransaction saved = transactions.save(PurchaseTransaction.newPurchase(
            "Office supplies",
            LocalDate.of(2026, 5, 1),
            new BigDecimal("123.45")
        ));

        assertThat(saved.id()).isNotNull();

        PurchaseTransaction reloaded = transactions.findById(saved.id()).orElseThrow();
        assertThat(reloaded.description()).isEqualTo("Office supplies");
        assertThat(reloaded.transactionDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(reloaded.purchaseAmountUsd()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(reloaded.createdAt()).isNotNull();
    }

    @Test
    void rejectsNonPositivePurchaseAmount() {
        assertThatThrownBy(() -> transactions.save(PurchaseTransaction.newPurchase(
            "Bad amount", LocalDate.of(2026, 5, 1), new BigDecimal("-1.00")
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> transactions.save(PurchaseTransaction.newPurchase(
            "   ", LocalDate.of(2026, 5, 1), new BigDecimal("10.00")
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsOutboxEventInPendingStatus() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent saved = outbox.save(OutboxEvent.pending(
            aggregateId,
            "purchase.transaction.created",
            "{\"id\":\"%s\"}".formatted(aggregateId)
        ));

        OutboxEvent reloaded = outbox.findById(saved.id()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(reloaded.aggregateId()).isEqualTo(aggregateId);
        assertThat(reloaded.payload()).contains(aggregateId.toString());
        assertThat(reloaded.publishedAt()).isNull();
    }

    @Test
    void pagedFinderOrdersByMostRecentTransactionDate() {
        transactions.save(PurchaseTransaction.newPurchase(
            "Older", LocalDate.of(2026, 1, 1), new BigDecimal("10.00")));
        transactions.save(PurchaseTransaction.newPurchase(
            "Newer", LocalDate.of(2026, 5, 1), new BigDecimal("20.00")));

        var page = transactions.findPage(10, 0);

        assertThat(page).extracting(PurchaseTransaction::description)
            .startsWith("Newer");
    }
}
