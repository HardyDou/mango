package io.mango.payment.starter;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.service.PaymentNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class PaymentNotificationDispatchScheduler implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentNotificationDispatchScheduler.class);

    private final PaymentNotificationRecordMapper notificationRecordMapper;
    private final PaymentNotificationService notificationService;
    private final int tenantLimit;
    private final int batchSize;
    private final ScheduledFuture<?> future;

    public PaymentNotificationDispatchScheduler(
            PaymentNotificationRecordMapper notificationRecordMapper,
            PaymentNotificationService notificationService,
            TaskScheduler taskScheduler,
            long intervalMillis,
            long initialDelayMillis,
            int tenantLimit,
            int batchSize) {
        Require.notNull(notificationRecordMapper, "支付通知记录 Mapper 不能为空");
        Require.notNull(notificationService, "支付通知服务不能为空");
        Require.notNull(taskScheduler, "支付通知调度器不能为空");
        Require.isTrue(intervalMillis > 0, "支付通知投递间隔必须大于 0");
        Require.isTrue(initialDelayMillis >= 0, "支付通知初始延迟不能为负数");
        Require.isTrue(tenantLimit > 0 && tenantLimit <= 100, "支付通知租户批次大小必须在 1 到 100 之间");
        Require.isTrue(batchSize > 0 && batchSize <= 100, "支付通知投递批次大小必须在 1 到 100 之间");
        this.notificationRecordMapper = notificationRecordMapper;
        this.notificationService = notificationService;
        this.tenantLimit = tenantLimit;
        this.batchSize = batchSize;
        PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(intervalMillis));
        trigger.setInitialDelay(Duration.ofMillis(initialDelayMillis));
        this.future = taskScheduler.schedule(this::dispatchSafely, trigger);
    }

    public int dispatchOnce() {
        List<Long> tenantIds = notificationRecordMapper.selectDueNotificationTenantIds(LocalDateTime.now(), tenantLimit);
        int total = 0;
        for (Long tenantId : tenantIds) {
            if (tenantId != null) {
                total += dispatchTenant(tenantId);
            }
        }
        return total;
    }

    @Override
    public void close() {
        if (future != null) {
            future.cancel(false);
        }
    }

    private void dispatchSafely() {
        try {
            int count = dispatchOnce();
            if (count > 0) {
                LOGGER.debug("Payment notification dispatched: count={}", count);
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Payment notification dispatch failed", ex);
        }
    }

    private int dispatchTenant(Long tenantId) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(MangoContextSnapshot.empty()
                    .withSecurity(null, String.valueOf(tenantId), "payment-notification-scheduler",
                            "SYSTEM", "SYSTEM_TASK", "SYSTEM", null, "mango-payment"));
            return notificationService.deliverDueNotificationRecords(batchSize);
        } finally {
            MangoContextHolder.set(previous);
        }
    }
}
