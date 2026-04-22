package com.jhpark.tradecore.db.entity.outbox;

import com.jhpark.tradecore.core.application.outbox.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false, length = 64)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            OutboxStatus status,
            int attemptCount,
            String lastError,
            Instant createdAt,
            Instant publishedAt
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId is null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType is null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId is null");
        this.eventType = Objects.requireNonNull(eventType, "eventType is null");
        this.payload = Objects.requireNonNull(payload, "payload is null");
        this.status = Objects.requireNonNull(status, "status is null");
        this.attemptCount = attemptCount;
        this.lastError = lastError;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is null");
        this.publishedAt = publishedAt;
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt is null");
        this.lastError = null;
    }

    public void markFailed(String lastError) {
        this.attemptCount += 1;
        this.lastError = lastError;
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

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
