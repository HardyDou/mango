package io.mango.workflow.starter.notice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowNoticeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WorkflowNoticeAutoConfiguration.class));

    @Test
    void defaultConfigurationRegistersWorkflowNoticeSubscriber() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WorkflowNoticeDomainEventSubscriber.class);
        });
    }

    @Test
    void noticeSwitchOffDoesNotRegisterWorkflowNoticeSubscriber() {
        runner.withPropertyValues("mango.workflow.notice.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WorkflowNoticeDomainEventSubscriber.class);
                });
    }

    @Test
    void disabledWorkflowDoesNotRegisterWorkflowNoticeSubscriber() {
        runner.withPropertyValues("mango.workflow.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WorkflowNoticeDomainEventSubscriber.class);
                });
    }
}
