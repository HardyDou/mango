package io.mango.payment.starter;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.payment.core.mapper.PaymentNotificationRecordMapper;
import io.mango.payment.core.service.PaymentNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentNotificationDispatchSchedulerTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("dispatchOnce should dispatch due notifications per tenant context")
    void dispatchOnce_dispatchesPerTenantContext() {
        TestPaymentNotificationRecordMapper mapper = new TestPaymentNotificationRecordMapper(List.of(1L, 2L));
        TestPaymentNotificationService notificationService = new TestPaymentNotificationService();
        TestScheduledFuture future = new TestScheduledFuture();
        TaskScheduler taskScheduler = new TestTaskScheduler(future);
        PaymentNotificationDispatchScheduler scheduler = new PaymentNotificationDispatchScheduler(
                mapper.proxy(),
                notificationService.proxy(),
                taskScheduler,
                60_000L,
                0L,
                20,
                10);

        int count = scheduler.dispatchOnce();
        scheduler.close();

        assertThat(count).isEqualTo(5);
        assertThat(MangoContextHolder.get().isEmpty()).isTrue();
        assertThat(notificationService.tenantIds).containsExactly("1", "2");
        assertThat(notificationService.limits).containsExactly(10L, 10L);
        assertThat(future.cancelled).isTrue();
    }

    private static class TestPaymentNotificationRecordMapper {

        private final List<Long> tenantIds;

        TestPaymentNotificationRecordMapper(List<Long> tenantIds) {
            this.tenantIds = tenantIds;
        }

        PaymentNotificationRecordMapper proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("selectDueNotificationTenantIds".equals(method.getName())) {
                    return tenantIds;
                }
                if ("toString".equals(method.getName())) {
                    return "TestPaymentNotificationRecordMapper";
                }
                throw new AssertionError(method.getName());
            };
            return (PaymentNotificationRecordMapper) Proxy.newProxyInstance(
                    PaymentNotificationRecordMapper.class.getClassLoader(),
                    new Class<?>[]{PaymentNotificationRecordMapper.class},
                    handler);
        }
    }

    private static class TestPaymentNotificationService extends PaymentNotificationService {

        private final List<String> tenantIds = new ArrayList<>();
        private final List<Long> limits = new ArrayList<>();

        TestPaymentNotificationService() {
            super(null, null, null, null, null, null, null, null);
        }

        PaymentNotificationService proxy() {
            return this;
        }

        @Override
        public int deliverDueNotificationRecords(long limit) {
            tenantIds.add(MangoContextHolder.tenantId());
            limits.add(limit);
            return "1".equals(MangoContextHolder.tenantId()) ? 2 : 3;
        }
    }

    private static class TestScheduledFuture implements ScheduledFuture<Object> {

        private boolean cancelled;

        @Override
        public long getDelay(java.util.concurrent.TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, java.util.concurrent.TimeUnit unit) {
            return null;
        }
    }

    private static class TestTaskScheduler implements TaskScheduler {

        private final ScheduledFuture<?> future;

        TestTaskScheduler(ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return future;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            return future;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            return future;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            return future;
        }
    }
}
