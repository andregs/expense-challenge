package com.example.expensechallenge.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

@Table("outbox_events")
public record OutboxEvent(
    @Id UUID id,
    UUID aggregateId,
    String type,
    String payload,
    OutboxStatus status,
    @ReadOnlyProperty OffsetDateTime createdAt,
    OffsetDateTime publishedAt,
    int retryCount
) {
    public static OutboxEvent pending(UUID aggregateId, String type, String payload) {
        return new OutboxEvent(null, aggregateId, type, payload, OutboxStatus.PENDING, null, null, 0);
    }

    public OutboxEvent markPublished(OffsetDateTime at) {
        return new OutboxEvent(id, aggregateId, type, payload, OutboxStatus.PUBLISHED, createdAt, at, retryCount);
    }

    public OutboxEvent incrementRetry() {
        return new OutboxEvent(id, aggregateId, type, payload, OutboxStatus.PENDING, createdAt, null, retryCount + 1);
    }

    public OutboxEvent markFailed() {
        return new OutboxEvent(id, aggregateId, type, payload, OutboxStatus.FAILED, createdAt, null, retryCount);
    }
}
