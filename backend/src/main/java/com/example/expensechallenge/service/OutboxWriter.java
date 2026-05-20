package com.example.expensechallenge.service;

import com.example.expensechallenge.api.dto.MoneyFormatter;
import com.example.expensechallenge.domain.OutboxEvent;
import com.example.expensechallenge.domain.PurchaseTransaction;
import com.example.expensechallenge.infrastructure.persistence.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OutboxWriter {

    static final String EVENT_TYPE = "purchase.transaction.created";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OutboxWriter(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(PurchaseTransaction tx) {
        var payload = new OutboxPayload(
            tx.id().toString(),
            tx.description(),
            tx.transactionDate().toString(),
            MoneyFormatter.format(tx.purchaseAmountUsd())
        );
        String json = jsonMapper.writeValueAsString(payload);
        outboxEventRepository.save(OutboxEvent.pending(tx.id(), EVENT_TYPE, json));
    }

    record OutboxPayload(
        String id,
        String description,
        String transactionDate,
        String purchaseAmountUsd
    ) {}
}
