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
    void 不同任务键可以并行执行() throws Exception {
        ConversionCoordinator coordinator = new ConversionCoordinator();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> first = coordinator.submit("file-a|regular", Duration.ofSeconds(2),
                () -> awaitAndReturn(started, release, "a"));
        CompletableFuture<String> second = coordinator.submit("file-b|regular", Duration.ofSeconds(2),
                () -> awaitAndReturn(started, release, "b"));
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        assertThat(first.get(1, TimeUnit.SECONDS)).isEqualTo("a");
        assertThat(second.get(1, TimeUnit.SECONDS)).isEqualTo("b");
    }

    @Test
    void 默认最多同时运行两个转换任务() throws Exception {
        ConversionCoordinator coordinator = new ConversionCoordinator();
        CountDownLatch firstTwoStarted = new CountDownLatch(2);
        CountDownLatch thirdStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> first = coordinator.submit("file-1", Duration.ofSeconds(2),
                () -> awaitAndReturn(firstTwoStarted, release, "1"));
        CompletableFuture<String> second = coordinator.submit("file-2", Duration.ofSeconds(2),
                () -> awaitAndReturn(firstTwoStarted, release, "2"));
        CompletableFuture<String> third = coordinator.submit("file-3", Duration.ofSeconds(2), () -> {
            thirdStarted.countDown();
            return "3";
        });
        assertThat(firstTwoStarted.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(thirdStarted.await(100, TimeUnit.MILLISECONDS)).isFalse();
        release.countDown();
        assertThat(first.get(1, TimeUnit.SECONDS)).isEqualTo("1");
        assertThat(second.get(1, TimeUnit.SECONDS)).isEqualTo("2");
        assertThat(third.get(1, TimeUnit.SECONDS)).isEqualTo("3");
    }

    @Test
    void 请求超时后底层任务仍在途时同一键不会再次执行() throws Exception {
        ConversionCoordinator coordinator = new ConversionCoordinator();
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        CompletableFuture<String> timedOut = coordinator.submit("timeout", Duration.ofMillis(30), () -> {
            executions.incrementAndGet();
            try {
                assertThat(release.await(1, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
            return "done";
        });
        assertThatThrownBy(timedOut::join).hasRootCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        assertThat(coordinator.inFlight()).isEqualTo(1);
        CompletableFuture<String> second = coordinator.submit("timeout", Duration.ofSeconds(1), () -> "retry");
        release.countDown();
        assertThat(second.join()).isEqualTo("done");
        assertThat(executions).hasValue(1);
    }

    private String awaitAndReturn(CountDownLatch started, CountDownLatch release, String value) {
        started.countDown();
        try {
            assertThat(release.await(1, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
        return value;
    }
}
