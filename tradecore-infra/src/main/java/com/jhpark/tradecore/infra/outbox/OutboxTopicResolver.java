package com.jhpark.tradecore.infra.outbox;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class OutboxTopicResolver {

    private final OutboxTopicsProperties topics;

    public OutboxTopicResolver(OutboxTopicsProperties topics) {
        this.topics = Objects.requireNonNull(topics, "topics is null");
    }

    public String resolve(String eventType) {
        return switch (eventType) {
            case "ORDER_PLACED" -> topics.getOrderPlaced();
            case "ORDER_CANCELLED" -> topics.getOrderCancelled();
            case "EXECUTION_APPLIED" -> topics.getExecutionApplied();
            default -> throw new IllegalArgumentException("지원하지 않는 outbox eventType 입니다. eventType=" + eventType);
        };
    }
}
