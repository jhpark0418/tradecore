package com.jhpark.tradecore.infra.outbox;

import com.jhpark.tradecore.core.application.outbox.OutboxEvent;
import com.jhpark.tradecore.core.application.port.out.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;


@Component
@ConditionalOnProperty(
        prefix = "tradecore.outbox.relay",
        name = "enabled",
        havingValue = "true"
)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher publisher;
    private final OutboxTopicResolver topicResolver;
    private final int batchSize;

    public OutboxRelay(
            OutboxEventRepository outboxEventRepository,
            OutboxEventPublisher publisher,
            OutboxTopicResolver topicResolver,
            @Value("${tradecore.outbox.relay.batch-size:20}") int batchSize
    ) {
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository, "outboxEventRepository is null");
        this.publisher = Objects.requireNonNull(publisher, "publisher is null");
        this.topicResolver = Objects.requireNonNull(topicResolver, "topicResolver is null");
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${tradecore.outbox.relay.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPending(batchSize);

        if (events.isEmpty()) {
            return;
        }

        log.info("Outbox relay started. pendingCount={}", events.size());

        for (OutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            String topic = topicResolver.resolve(event.getEventType());
            String key = event.getAggregateId();

            publisher.publish(topic, key, event.getPayload());
            outboxEventRepository.markPublished(event.getEventId(), Instant.now());

            log.info(
                    "Outbox event published. eventId={}, eventType={}, topic={}, key={}",
                    event.getEventId(),
                    event.getEventType(),
                    topic,
                    key
            );
        } catch (Exception e) {
            String errorMessage = truncateErrorMessage(e);

            outboxEventRepository.markFailed(event.getEventId(), errorMessage);

            log.warn(
                    "Outbox event publish failed. eventId={}, eventType={}, error={}",
                    event.getEventId(),
                    event.getEventType(),
                    errorMessage,
                    e
            );
        }
    }

    private String truncateErrorMessage(Exception e) {
        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }

        int maxLength = 200;
        if (message.length() <= maxLength) {
            return message;
        }

        return message.substring(0, maxLength);
    }
}
