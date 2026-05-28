package io.mango.notice.starter;

import io.mango.infra.kv.api.IOutboxDispatcher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.notice.core.service.INoticeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NoticeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NoticeOutboxAutoConfiguration.class))
            .withUserConfiguration(TestBeans.class);

    @Test
    void whenOutboxStoreExists_createsDispatcherAndWorker() {
        runner.withPropertyValues(
                        "mango.notice.outbox.worker-id=test-worker",
                        "mango.notice.outbox.initial-delay-millis=60000",
                        "mango.notice.outbox.fixed-delay-millis=60000")
                .run(context -> {
                    assertThat(context).hasSingleBean(IOutboxDispatcher.class);
                    assertThat(context).hasSingleBean(NoticeOutboxWorker.class);
                    int result = context.getBean(NoticeOutboxWorker.class).dispatchOnce();
                    assertThat(result).isZero();
                });
    }

    @Test
    void whenDispatchDisabled_createsDispatcherWithoutWorker() {
        runner.withPropertyValues("mango.notice.outbox.dispatch-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(IOutboxDispatcher.class);
                    assertThat(context).doesNotHaveBean(NoticeOutboxWorker.class);
                });
    }

    @Test
    void whenOutboxDisabled_skipsDispatcherAndWorker() {
        runner.withPropertyValues("mango.notice.outbox.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IOutboxDispatcher.class);
                    assertThat(context).doesNotHaveBean(NoticeOutboxWorker.class);
                });
    }

    @Configuration
    static class TestBeans {

        @Bean
        IOutboxStore outboxStore() {
            return mock(IOutboxStore.class);
        }

        @Bean
        INoticeService noticeService() {
            return mock(INoticeService.class);
        }
    }
}
