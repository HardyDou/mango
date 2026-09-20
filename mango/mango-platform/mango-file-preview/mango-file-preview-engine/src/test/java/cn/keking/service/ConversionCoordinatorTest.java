package cn.keking.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionCoordinatorTest {
    @Test
    void 同一任务键并发提交只执行一次并复用结果() throws Exception {
        ConversionCoordinator coordinator = new ConversionCoordinator();
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> first = coordinator.submit("same", Duration.ofSeconds(2), () -> {
            executions.incrementAndGet();
            started.countDown();
            try {
                assertThat(release.await(1, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
            return "pdf";
        });
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<String> second = coordinator.submit("same", Duration.ofSeconds(2), () -> {
            executions.incrementAndGet();
            return "other";
        });
        release.countDown();
        assertThat(first.get(1, TimeUnit.SECONDS)).isEqualTo("pdf");
        assertThat(second.get(1, TimeUnit.SECONDS)).isEqualTo("pdf");
        assertThat(executions).hasValue(1);
    }

    @Test
    void 超时后任务失败且同一键可以重新提交() {
        ConversionCoordinator coordinator = new ConversionCoordinator();
        CompletableFuture<String> timedOut = coordinator.submit("timeout", Duration.ofMillis(30), () -> {
            try { Thread.sleep(200); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            return "never";
        });
        assertThatThrownBy(timedOut::join).hasRootCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        assertThat(coordinator.inFlight()).isZero();
        assertThat(coordinator.submit("timeout", Duration.ofSeconds(1), () -> "retry").join()).isEqualTo("retry");
    }
}
