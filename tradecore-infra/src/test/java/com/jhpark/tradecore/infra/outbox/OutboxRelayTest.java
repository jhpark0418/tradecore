package com.jhpark.tradecore.infra.outbox;

import com.jhpark.tradecore.core.application.outbox.OutboxEvent;
import com.jhpark.tradecore.core.application.outbox.OutboxStatus;
import com.jhpark.tradecore.core.support.fake.FakeOutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayTest {
    @Test
    void pending_outbox_event를_publish_후_published로_변경한다() {
        // given
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository();
        RecordingOutboxEventPublisher publisher = new RecordingOutboxEventPublisher();
        OutboxTopicResolver topicResolver = new OutboxTopicResolver(new OutboxTopicsProperties());

        OutboxRelay relay = new OutboxRelay(
                repository,
                publisher,
                topicResolver,
                20
        );

        repository.save(pendingEvent(
                "event-1",
                "order-1",
                "ORDER_PLACED",
                """
                {"orderId":"order-1"}
                """
        ));

        // when
        relay.publishPendingEvents();

        // then
        assertThat(publisher.publishedMessages).hasSize(1);

        PublishedMessage message = publisher.publishedMessages.getFirst();
        assertThat(message.topic()).isEqualTo("tradecore.order.placed");
        assertThat(message.key()).isEqualTo("order-1");
        assertThat(message.payload()).contains("\"orderId\":\"order-1\"");

        assertThat(repository.get(0).getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(repository.getFailedRecords()).isEmpty();
    }

    @Test
    void publish_실패시_failed_record를_남기고_pending_상태를_유지한다() {
        // given
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository();
        RecordingOutboxEventPublisher publisher = new RecordingOutboxEventPublisher();
        publisher.fail = true;

        OutboxTopicResolver topicResolver = new OutboxTopicResolver(new OutboxTopicsProperties());

        OutboxRelay relay = new OutboxRelay(
                repository,
                publisher,
                topicResolver,
                20
        );

        repository.save(pendingEvent(
                "event-1",
                "order-1",
                "ORDER_PLACED",
                """
                {"orderId":"order-1"}
                """
        ));

        // when
        relay.publishPendingEvents();

        // then
        assertThat(publisher.publishedMessages).isEmpty();

        assertThat(repository.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(repository.getFailedRecords()).hasSize(1);
        assertThat(repository.getFailedRecords().getFirst().eventId()).isEqualTo("event-1");
        assertThat(repository.getFailedRecords().getFirst().lastError())
                .contains("publisher failure");
    }

    @Test
    void batch_size_만큼만_pending_event를_발행한다() {
        // given
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository();
        RecordingOutboxEventPublisher publisher = new RecordingOutboxEventPublisher();
        OutboxTopicResolver topicResolver = new OutboxTopicResolver(new OutboxTopicsProperties());

        OutboxRelay relay = new OutboxRelay(
                repository,
                publisher,
                topicResolver,
                2
        );

        repository.save(pendingEvent("event-1", "order-1", "ORDER_PLACED", "{\"orderId\":\"order-1\"}"));
        repository.save(pendingEvent("event-2", "order-2", "ORDER_CANCELLED", "{\"orderId\":\"order-2\"}"));
        repository.save(pendingEvent("event-3", "execution-1", "EXECUTION_APPLIED", "{\"executionId\":\"execution-1\"}"));

        // when
        relay.publishPendingEvents();

        // then
        assertThat(publisher.publishedMessages).hasSize(2);

        assertThat(repository.get(0).getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(repository.get(1).getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(repository.get(2).getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void 지원하지_않는_eventType이면_failed_record를_남긴다() {
        // given
        FakeOutboxEventRepository repository = new FakeOutboxEventRepository();
        RecordingOutboxEventPublisher publisher = new RecordingOutboxEventPublisher();
        OutboxTopicResolver topicResolver = new OutboxTopicResolver(new OutboxTopicsProperties());

        OutboxRelay relay = new OutboxRelay(
                repository,
                publisher,
                topicResolver,
                20
        );

        repository.save(pendingEvent(
                "event-1",
                "order-1",
                "UNKNOWN_EVENT",
                "{\"orderId\":\"order-1\"}"
        ));

        // when
        relay.publishPendingEvents();

        // then
        assertThat(publisher.publishedMessages).isEmpty();

        assertThat(repository.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(repository.getFailedRecords()).hasSize(1);
        assertThat(repository.getFailedRecords().getFirst().lastError())
                .contains("지원하지 않는 outbox eventType");
    }

    private OutboxEvent pendingEvent(
            String eventId,
            String aggregateId,
            String eventType,
            String payload
    ) {
        return new OutboxEvent(
                eventId,
                "ORDER",
                aggregateId,
                eventType,
                payload,
                OutboxStatus.PENDING,
                Instant.now()
        );
    }

    private static class RecordingOutboxEventPublisher implements OutboxEventPublisher {

        private final List<PublishedMessage> publishedMessages = new ArrayList<>();
        private boolean fail = false;

        @Override
        public void publish(String topic, String key, String payload) {
            if (fail) {
                throw new IllegalStateException("publisher failure");
            }

            publishedMessages.add(new PublishedMessage(topic, key, payload));
        }
    }

    private record PublishedMessage(
            String topic,
            String key,
            String payload
    ) {
    }
}