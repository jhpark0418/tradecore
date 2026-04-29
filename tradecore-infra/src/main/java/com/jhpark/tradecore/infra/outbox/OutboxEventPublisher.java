package com.jhpark.tradecore.infra.outbox;

public interface OutboxEventPublisher {
    void publish(String topic, String key, String payload);
}
