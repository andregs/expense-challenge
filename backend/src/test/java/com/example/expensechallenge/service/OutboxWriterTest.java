package com.example.expensechallenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.expensechallenge.domain.OutboxEvent;
import com.example.expensechallenge.domain.OutboxStatus;
import com.example.expensechallenge.domain.PurchaseTransaction;
import com.example.expensechallenge.infrastructure.persistence.OutboxEventRepository;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link OutboxWriter}. Verifies the payload JSON shape, the
 * event type constant, and the {@link Propagation#MANDATORY} contract (calling
 * {@code write} outside an active transaction must throw). The propagation
 * contract is verified via reflection since the AOP proxy is absent in a plain
 * unit test; end-to-end enforcement is covered by
 * {@code OutboxIntegrationTest}.
 */
class OutboxWriterTest {

    private OutboxEventRepository repository;
    private OutboxWriter writer;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        jsonMapper = JsonMapper.builder().build();
        writer = new OutboxWriter(repository, jsonMapper);
    }

    @Test
    void eventType_matchesExpectedConstant() {
        assertThat(OutboxWriter.EVENT_TYPE).isEqualTo("purchase.transaction.created");
    }

    @Test
    void write_savesEventWithCorrectTypeAndStatus() {
        UUID id = UUID.randomUUID();
        PurchaseTransaction tx = new PurchaseTransaction(
                id, "Hotel stay", LocalDate.of(2026, 4, 15),
                new BigDecimal("199.50"), null);

        writer.write(tx);

        ArgumentCaptor<OutboxEvent> captor = forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertThat(saved.type()).isEqualTo(OutboxWriter.EVENT_TYPE);
        assertThat(saved.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.aggregateId()).isEqualTo(id);
        assertThat(saved.retryCount()).isZero();
    }

    @Test
    void write_payloadContainsAllFields() {
        UUID id = UUID.randomUUID();
        PurchaseTransaction tx = new PurchaseTransaction(
                id, "Office supplies", LocalDate.of(2026, 4, 15),
                new BigDecimal("124.99"), null);

        writer.write(tx);

        ArgumentCaptor<OutboxEvent> captor = forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        String json = captor.getValue().payload();

        ObjectNode node = (ObjectNode) jsonMapper.readTree(json);
        assertThat(node.get("id").stringValue()).isEqualTo(id.toString());
        assertThat(node.get("description").stringValue()).isEqualTo("Office supplies");
        assertThat(node.get("transactionDate").stringValue()).isEqualTo("2026-04-15");
        assertThat(node.get("purchaseAmountUsd").stringValue()).isEqualTo("124.99");
    }

    @Test
    void write_roundsAmountToTwoDecimalPlacesHalfUp() {
        UUID id = UUID.randomUUID();
        PurchaseTransaction tx = new PurchaseTransaction(
                id, "Rounding test", LocalDate.of(2026, 4, 15),
                new BigDecimal("9.999"), null);

        writer.write(tx);

        ArgumentCaptor<OutboxEvent> captor = forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().payload()).contains("\"purchaseAmountUsd\":\"10.00\"");
    }

    @Test
    void write_hasTransactionalMandatoryAnnotation() throws NoSuchMethodException {
        // Verify the propagation contract at the annotation level.
        // Runtime enforcement (throwing IllegalTransactionStateException when no
        // active transaction is present) is covered by OutboxIntegrationTest.
        Transactional annotation = OutboxWriter.class
                .getMethod("write", PurchaseTransaction.class)
                .getAnnotation(Transactional.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.propagation()).isEqualTo(Propagation.MANDATORY);
    }
}
