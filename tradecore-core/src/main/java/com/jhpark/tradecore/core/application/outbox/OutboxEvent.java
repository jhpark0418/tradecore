package com.jhpark.tradecore.core.application.outbox;

import java.time.Instant;
import java.util.Objects;

public class OutboxEvent {

    private final String eventId;
    private final String aggregateType;
    private final String aggregateId;
    private final String eventType;
    private final String payload;
    private final OutboxStatus status;
    private final Instant createdAt;

    public OutboxEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        Instant createdAt
    ) {
        this.eventId = requireText(eventId, "eventId");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = requireText(aggregateId, "aggregateId");
        this.eventType = requireText(eventType, "eventType");
        this.payload = requireText(payload, "payload");
        this.status = Objects.requireNonNull(status, "status is null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is null");
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is null or blank");
        }

        return value;
    }
}
