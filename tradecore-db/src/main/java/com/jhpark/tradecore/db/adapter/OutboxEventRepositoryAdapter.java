package com.jhpark.tradecore.db.adapter;

import com.jhpark.tradecore.core.application.outbox.OutboxEvent;
import com.jhpark.tradecore.core.application.outbox.OutboxStatus;
import com.jhpark.tradecore.core.application.port.out.OutboxEventRepository;
import com.jhpark.tradecore.db.entity.outbox.OutboxEventEntity;
import com.jhpark.tradecore.db.repository.OutboxEventJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    public OutboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
    }

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity saved = outboxEventJpaRepository.saveAndFlush(toEntity(event));
        return toDomain(saved);
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return outboxEventJpaRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(String eventId, Instant publishedAt) {
        OutboxEventEntity entity = outboxEventJpaRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("outbox event not found. eventId=" + eventId));

        entity.markPublished(publishedAt);
    }

    @Override
    @Transactional
    public void markFailed(String eventId, String lastError) {
        OutboxEventEntity entity = outboxEventJpaRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("outbox event not found. eventId=" + eventId));

        entity.markFailed(lastError);
    }

    private OutboxEventEntity toEntity(OutboxEvent event) {
        return new OutboxEventEntity(
                event.getEventId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus(),
                0,
                null,
                event.getCreatedAt(),
                null
        );
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.getEventId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
