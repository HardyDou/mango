package io.mango.payment.starter;

import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.service.PaymentNotificationDispatcher;
import io.mango.payment.core.service.PaymentRefundApprovalWorkflowPublisher;
import io.mango.payment.starter.workflow.PaymentRefundApprovalWorkflowDefinitionInitializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Runtime-only Payment schedulers and startup initializers. */
@AutoConfiguration(after = PaymentAutoConfiguration.class)
@ConditionalOnBean(PaymentProperties.class)
@ConditionalOnProperty(prefix = "mango.bootstrap", name = "mode", havingValue = "runtime",
        matchIfMissing = true)
public class PaymentRuntimeAutoConfiguration {

    private static final int TASK_TERMINATION_TIMEOUT_SECONDS = 10;

    @Bean(name = "paymentNotificationTaskScheduler")
    @ConditionalOnBean({PaymentNotificationRecordMapper.class, PaymentNotificationDispatcher.class})
    @ConditionalOnProperty(prefix = "mango.payment.notification.dispatch", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public TaskScheduler paymentNotificationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("payment-notification-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(TASK_TERMINATION_TIMEOUT_SECONDS);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnBean({PaymentNotificationRecordMapper.class, PaymentNotificationDispatcher.class})
    @ConditionalOnProperty(prefix = "mango.payment.notification.dispatch", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public PaymentNotificationDispatchScheduler paymentNotificationDispatchScheduler(
            PaymentNotificationRecordMapper notificationRecordMapper,
            PaymentNotificationDispatcher notificationService,
            @Qualifier("paymentNotificationTaskScheduler") TaskScheduler taskScheduler,
            PaymentProperties properties) {
        PaymentProperties.NotificationDispatchProperties dispatch = properties.getNotification().getDispatch();
        return new PaymentNotificationDispatchScheduler(
                notificationRecordMapper,
                notificationService,
                taskScheduler,
                dispatch.getIntervalMillis(),
                dispatch.getInitialDelayMillis(),
                dispatch.getTenantLimit(),
                dispatch.getBatchSize());
    }

    @Bean
    @ConditionalOnBean(PaymentRefundApprovalWorkflowPublisher.class)
    @ConditionalOnProperty(prefix = "mango.payment.workflow.refund-approval.initializer", name = "enabled",
            havingValue = "true")
    public PaymentRefundApprovalWorkflowDefinitionInitializer paymentRefundApprovalWorkflowDefinitionInitializer(
            PaymentRefundApprovalWorkflowPublisher workflowPublisher,
            PaymentProperties properties) {
        return new PaymentRefundApprovalWorkflowDefinitionInitializer(workflowPublisher, properties);
    }
}
