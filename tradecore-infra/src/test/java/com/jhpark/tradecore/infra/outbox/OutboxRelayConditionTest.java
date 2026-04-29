package com.jhpark.tradecore.infra.outbox;

import com.jhpark.tradecore.core.application.port.out.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxRelayConditionTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void enabled_설정이_없으면_OutboxRelay가_등록되지_않는다() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(OutboxRelay.class);
        });
    }

    @Test
    void enabled_false이면_OutboxRelay가_등록되지_않는다() {
        contextRunner
                .withPropertyValues("tradecore.outbox.relay.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxRelay.class);
                });
    }

    @Test
    void enabled_true이면_OutboxRelay가_등록된다() {
        contextRunner
                .withPropertyValues(
                        "tradecore.outbox.relay.enabled=true",
                        "tradecore.outbox.relay.batch-size=20",
                        "tradecore.outbox.relay.fixed-delay-ms=5000"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxRelay.class);
                    assertThat(context).hasSingleBean(OutboxTopicResolver.class);
                    assertThat(context).hasSingleBean(OutboxEventPublisher.class);
                    assertThat(context).hasSingleBean(OutboxEventRepository.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            OutboxRelay.class,
            OutboxTopicResolver.class,
            OutboxTopicsProperties.class
    })
    static class TestConfig {

        @Bean
        OutboxEventRepository outboxEventRepository() {
            return mock(OutboxEventRepository.class);
        }

        @Bean
        OutboxEventPublisher outboxEventPublisher() {
            return mock(OutboxEventPublisher.class);
        }
    }
}
