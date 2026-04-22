package com.jhpark.tradecore.core.application.port.out;

import com.jhpark.tradecore.core.application.outbox.OutboxEvent;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);

    void markPublished(String eventId, Instant publishedAt);

    void markFailed(String eventId, String lastError);
}
