package io.mango.payment.starter;

import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.service.PaymentNotificationService;
import io.mango.payment.core.service.PaymentObservabilityProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@AutoConfiguration
@AutoConfigureAfter(name = "io.mango.workflow.starter.WorkflowAutoConfiguration")
@ConditionalOnProperty(prefix = "mango.payment", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PaymentProperties.class)
@MapperScan("io.mango.payment.core.mapper")
@ComponentScan({
        "io.mango.payment.core.service",
        "io.mango.payment.starter.notice",
        "io.mango.payment.starter.controller",
        "io.mango.payment.starter.resource",
        "io.mango.payment.starter.workflow"
})
public class PaymentAutoConfiguration {

    @Bean(name = "paymentNotificationTaskScheduler")
    @ConditionalOnProperty(prefix = "mango.payment.notification.dispatch", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TaskScheduler paymentNotificationTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("payment-notification-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.payment.notification.dispatch", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PaymentNotificationDispatchScheduler paymentNotificationDispatchScheduler(
            PaymentNotificationRecordMapper notificationRecordMapper,
            PaymentNotificationService notificationService,
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
    public PaymentObservabilityProperties paymentObservabilityProperties(PaymentProperties properties) {
        return properties.getObservability();
    }
}
