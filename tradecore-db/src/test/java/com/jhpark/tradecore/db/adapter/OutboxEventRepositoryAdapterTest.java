package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.application.outbox.OutboxEvent;
import com.jhpark.tradecore.core.application.outbox.OutboxStatus;
import com.jhpark.tradecore.db.DbTestApplication;
import com.jhpark.tradecore.db.entity.outbox.OutboxEventEntity;
import com.jhpark.tradecore.db.repository.OutboxEventJpaRepository;
import com.jhpark.tradecore.db.support.PostgresJpaTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ContextConfiguration(classes = DbTestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboxEventRepositoryAdapter.class)
class OutboxEventRepositoryAdapterTest extends PostgresJpaTestSupport {

    @Autowired
    private OutboxEventRepositoryAdapter outboxEventRepositoryAdapter;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Test
    void save() {
        OutboxEvent event = new OutboxEvent(
                "event-1",
                "ORDER",
                "order-1",
                "ORDER_PLACED",
                """
                {
                  "orderId":"order-1",
                  "accountId":"account-1",
                  "status":"NEW"
                }
                """,
                OutboxStatus.PENDING,
                Instant.parse("2026-04-22T10:15:30Z")
        );

        OutboxEvent saved = outboxEventRepositoryAdapter.save(event);
        Optional<OutboxEventEntity> found = outboxEventJpaRepository.findById("event-1");

        assertEquals("event-1", saved.getEventId());
        assertTrue(found.isPresent());
        assertEquals("ORDER", found.get().getAggregateType());
        assertEquals("order-1", found.get().getAggregateId());
        assertEquals("ORDER_PLACED", found.get().getEventType());
        assertEquals(OutboxStatus.PENDING, found.get().getStatus());
        assertEquals(0, found.get().getAttemptCount());
        assertEquals(Instant.parse("2026-04-22T10:15:30Z"), found.get().getCreatedAt());
        assertTrue(found.get().getPayload().contains("\"orderId\":\"order-1\""));
    }

    @Test
    void findPending() {
        outboxEventRepositoryAdapter.save(new OutboxEvent(
                "event-1",
                "ORDER",
                "order-1",
                "ORDER_PLACED",
                """
                {"orderId":"order-1"}
                """,
                OutboxStatus.PENDING,
                Instant.parse("2026-04-22T10:00:00Z")
        ));

        outboxEventRepositoryAdapter.save(new OutboxEvent(
                "event-2",
                "ORDER",
                "order-2",
                "ORDER_PLACED",
                """
                {"orderId":"order-2"}
                """,
                OutboxStatus.PENDING,
                Instant.parse("2026-04-22T10:01:00Z")
        ));

        outboxEventRepositoryAdapter.save(new OutboxEvent(
                "event-3",
                "ORDER",
                "order-3",
                "ORDER_PLACED",
                """
                {"orderId":"order-3"}
                """,
                OutboxStatus.PUBLISHED,
                Instant.parse("2026-04-22T10:02:00Z")
        ));

        var result = outboxEventRepositoryAdapter.findPending(10);

        assertEquals(2, result.size());
        assertEquals("event-1", result.get(0).getEventId());
        assertEquals("event-2", result.get(1).getEventId());
    }

    @Test
    void markPublished() {
        outboxEventRepositoryAdapter.save(new OutboxEvent(
                "event-1",
                "ORDER",
                "order-1",
                "ORDER_PLACED",
                """
                {"orderId":"order-1"}
                """,
                OutboxStatus.PENDING,
                Instant.parse("2026-04-22T10:00:00Z")
        ));

        Instant publishedAt = Instant.parse("2026-04-22T10:05:00Z");

        outboxEventRepositoryAdapter.markPublished("event-1", publishedAt);

        OutboxEventEntity found = outboxEventJpaRepository.findById("event-1").orElseThrow();

        assertEquals(OutboxStatus.PUBLISHED, found.getStatus());
        assertEquals(publishedAt, found.getPublishedAt());
    }

    @Test
    void markFailed() {
        outboxEventRepositoryAdapter.save(new OutboxEvent(
                "event-1",
                "ORDER",
                "order-1",
                "ORDER_PLACED",
                """
                {"orderId":"order-1"}
                """,
                OutboxStatus.PENDING,
                Instant.parse("2026-04-22T10:00:00Z")
        ));

        outboxEventRepositoryAdapter.markFailed("event-1", "kafka publish failed");

        OutboxEventEntity found = outboxEventJpaRepository.findById("event-1").orElseThrow();

        assertEquals(OutboxStatus.PENDING, found.getStatus());
        assertEquals(1, found.getAttemptCount());
        assertEquals("kafka publish failed", found.getLastError());
    }
}