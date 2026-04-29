package com.jhpark.tradecore.infra.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        prefix = "tradecore.outbox.relay",
        name = "enabled",
        havingValue = "true"
)
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaOutboxEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate is null");
    }

    @Override
    public void publish(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload)
                    .get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka 발행 중 인터럽트가 발생했습니다.", e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Kafka 발행에 실패했습니다. topic=" + topic + ", key=" + key,
                    e
            );
        }
    }
}
