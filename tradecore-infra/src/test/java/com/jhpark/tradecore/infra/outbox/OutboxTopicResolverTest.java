package com.jhpark.tradecore.infra.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OutboxTopicResolverTest {
    private final OutboxTopicResolver resolver = new OutboxTopicResolver(new OutboxTopicsProperties());

    @Test
    void ORDER_PLACED_topic을_반환한다() {
        assertThat(resolver.resolve("ORDER_PLACED"))
                .isEqualTo("tradecore.order.placed");
    }

    @Test
    void ORDER_CANCELLED_topic을_반환한다() {
        assertThat(resolver.resolve("ORDER_CANCELLED"))
                .isEqualTo("tradecore.order.cancelled");
    }

    @Test
    void EXECUTION_APPLIED_topic을_반환한다() {
        assertThat(resolver.resolve("EXECUTION_APPLIED"))
                .isEqualTo("tradecore.execution.applied");
    }

    @Test
    void 지원하지_않는_eventType이면_예외가_발생한다() {
        assertThatThrownBy(() -> resolver.resolve("UNKNOWN_EVENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 outbox eventType");
    }
}