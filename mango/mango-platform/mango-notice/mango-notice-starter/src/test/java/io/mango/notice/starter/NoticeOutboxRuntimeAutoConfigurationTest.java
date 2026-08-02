package io.mango.notice.starter;

import io.mango.infra.kv.api.IOutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NoticeOutboxRuntimeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NoticeOutboxRuntimeAutoConfiguration.class))
            .withBean(IOutboxDispatcher.class, () -> mock(IOutboxDispatcher.class))
            .withBean(NoticeProperties.class, NoticeProperties::new);

    @Test
    void shouldExcludeOutboxWorkerFromBootstrapMode() {
        runner.withPropertyValues("mango.bootstrap.mode=bootstrap")
                .run(context -> assertThat(context).doesNotHaveBean(NoticeOutboxWorker.class));
    }

    @Test
    void shouldRetainOutboxWorkerForRuntimeAndLegacyMode() {
        runner.withPropertyValues("mango.bootstrap.mode=runtime")
                .run(context -> assertThat(context).hasSingleBean(NoticeOutboxWorker.class));
        runner.run(context -> assertThat(context).hasSingleBean(NoticeOutboxWorker.class));
    }
}
