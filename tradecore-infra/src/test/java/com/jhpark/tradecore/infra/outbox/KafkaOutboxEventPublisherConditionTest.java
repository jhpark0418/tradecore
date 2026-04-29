package com.jhpark.tradecore.infra.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaOutboxEventPublisherConditionTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void enabled_설정이_없으면_KafkaOutboxEventPublisher가_등록되지_않는다() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(KafkaOutboxEventPublisher.class);
            assertThat(context).doesNotHaveBean(OutboxEventPublisher.class);
        });
    }

    @Test
    void enabled_false이면_KafkaOutboxEventPublisher가_등록되지_않는다() {
        contextRunner
                .withPropertyValues("tradecore.outbox.relay.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(KafkaOutboxEventPublisher.class);
                    assertThat(context).doesNotHaveBean(OutboxEventPublisher.class);
                });
    }

    @Test
    void enabled_true이고_KafkaTemplate이_있으면_KafkaOutboxEventPublisher가_등록된다() {
        contextRunner
                .withPropertyValues("tradecore.outbox.relay.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaOutboxEventPublisher.class);
                    assertThat(context).hasSingleBean(OutboxEventPublisher.class);
                    assertThat(context).hasSingleBean(KafkaTemplate.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(KafkaOutboxEventPublisher.class)
    static class TestConfig {

        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}
