package com.jhpark.tradecore.core.support.fake;

import com.jhpark.tradecore.core.application.outbox.OutboxEvent;
import com.jhpark.tradecore.core.application.outbox.OutboxStatus;
import com.jhpark.tradecore.core.application.port.out.OutboxEventRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FakeOutboxEventRepository implements OutboxEventRepository {

    private final List<OutboxEvent> storage = new ArrayList<>();
    private final List<FailedRecord> failedRecords = new ArrayList<>();

    @Override
    public OutboxEvent save(OutboxEvent event) {
        storage.add(event);
        return event;
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return storage.stream()
                .filter(event -> event.getStatus() == OutboxStatus.PENDING)
                .sorted(Comparator.comparing(OutboxEvent::getCreatedAt))
                .limit(limit)
                .toList();
    }

    @Override
    public void markPublished(String eventId, Instant publishedAt) {
        for (int i = 0; i < storage.size(); i++) {
            OutboxEvent event = storage.get(i);
            if (event.getEventId().equals(eventId)) {
                storage.set(i, new OutboxEvent(
                        event.getEventId(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getEventType(),
                        event.getPayload(),
                        OutboxStatus.PUBLISHED,
                        event.getCreatedAt()
                ));
                return;
            }
        }
        throw new IllegalArgumentException("outbox event not found. eventId=" + eventId);
    }

    @Override
    public void markFailed(String eventId, String lastError) {
        boolean exists = storage.stream()
                .anyMatch(event -> event.getEventId().equals(eventId));

        if (!exists) {
            throw new IllegalArgumentException("outbox event not found. eventId=" + eventId);
        }

        failedRecords.add(new FailedRecord(eventId, lastError));
    }

    public int size() {
        return storage.size();
    }

    public List<OutboxEvent> findAll() {
        return Collections.unmodifiableList(storage);
    }

    public OutboxEvent get(int index) {
        return storage.get(index);
    }

    public List<FailedRecord> getFailedRecords() {
        return Collections.unmodifiableList(failedRecords);
    }

    public record FailedRecord(String eventId, String lastError) {
    }
}
