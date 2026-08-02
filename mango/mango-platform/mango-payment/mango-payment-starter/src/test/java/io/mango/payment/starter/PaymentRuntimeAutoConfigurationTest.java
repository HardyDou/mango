package io.mango.payment.starter;

import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.service.PaymentNotificationDispatcher;
import io.mango.payment.core.service.PaymentRefundApprovalWorkflowPublisher;
import io.mango.payment.starter.workflow.PaymentRefundApprovalWorkflowDefinitionInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PaymentRuntimeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PaymentRuntimeAutoConfiguration.class))
            .withBean(PaymentNotificationRecordMapper.class, () -> mock(PaymentNotificationRecordMapper.class))
            .withBean(PaymentNotificationDispatcher.class, () -> mock(PaymentNotificationDispatcher.class))
            .withBean(PaymentRefundApprovalWorkflowPublisher.class,
                    () -> mock(PaymentRefundApprovalWorkflowPublisher.class))
            .withBean(PaymentProperties.class, PaymentProperties::new)
            .withPropertyValues("mango.payment.workflow.refund-approval.initializer.enabled=true");

    @Test
    void shouldExcludeSchedulersAndInitializersFromBootstrapMode() {
        runner.withPropertyValues("mango.bootstrap.mode=bootstrap")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TaskScheduler.class);
                    assertThat(context).doesNotHaveBean(PaymentNotificationDispatchScheduler.class);
                    assertThat(context).doesNotHaveBean(PaymentRefundApprovalWorkflowDefinitionInitializer.class);
                });
    }

    @Test
    void shouldRetainRuntimeHooksForRuntimeAndLegacyMode() {
        runner.withPropertyValues("mango.bootstrap.mode=runtime")
                .run(this::assertRuntimeHooks);
        runner.run(this::assertRuntimeHooks);
    }

    private void assertRuntimeHooks(org.springframework.context.ApplicationContext context) {
        assertThat(context.getBeansOfType(TaskScheduler.class)).hasSize(1);
        assertThat(context.getBeansOfType(PaymentNotificationDispatchScheduler.class)).hasSize(1);
        assertThat(context.getBeansOfType(PaymentRefundApprovalWorkflowDefinitionInitializer.class)).hasSize(1);
    }
}
