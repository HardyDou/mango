package cn.keking.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 合并同一转换键的并发请求，确保请求超时不会制造第二个底层转换。 */
@Component
public final class ConversionCoordinator {
    private static final int DEFAULT_CONVERSION_WORKERS = 2;
    private static final Duration MINIMUM_LEASE_DURATION = Duration.ofMinutes(30);
    private static final long LEASE_POLL_MILLIS = 50L;
    private static final long MIN_SLEEP_MILLIS = 1L;
    private final ConcurrentMap<String, CompletableFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Executor executor;
    private final ConversionTaskLease lease;
    private final ScheduledExecutorService timeoutScheduler;

    public ConversionCoordinator() {
        this(defaultConversionExecutor(), new LocalConversionTaskLease(), defaultTimeoutScheduler());
    }

    @Autowired
    public ConversionCoordinator(ConversionTaskLease lease) {
        this(defaultConversionExecutor(), lease, defaultTimeoutScheduler());
    }

    ConversionCoordinator(Executor executor) {
        this(executor, new LocalConversionTaskLease(), defaultTimeoutScheduler());
    }

    ConversionCoordinator(Executor executor, ConversionTaskLease lease) {
        this(executor, lease, defaultTimeoutScheduler());
    }

    ConversionCoordinator(Executor executor, ConversionTaskLease lease,
                          ScheduledExecutorService timeoutScheduler) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.lease = Objects.requireNonNull(lease, "lease");
        this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "timeoutScheduler");
    }

    public <T> CompletableFuture<T> submit(String key, Duration timeout, Supplier<T> task) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(task, "task");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        @SuppressWarnings("unchecked")
        CompletableFuture<T> shared = (CompletableFuture<T>) tasks.computeIfAbsent(key, ignored -> {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(
                    () -> runWithLease(key, timeout, task, leaseDuration(timeout)), executor);
            future.whenComplete((value, error) -> tasks.remove(key, future));
            return future;
        });
        return timeoutView(shared, timeout);
    }

    public int inFlight() {
        return tasks.size();
    }

    private <T> CompletableFuture<T> timeoutView(CompletableFuture<T> shared, Duration timeout) {
        CompletableFuture<T> view = new CompletableFuture<>();
        ScheduledFuture<?> timeoutFuture = timeoutScheduler.schedule(
                () -> view.completeExceptionally(new java.util.concurrent.TimeoutException(
                        "conversion timeout after " + timeout)),
                timeout.toMillis(), TimeUnit.MILLISECONDS);
        shared.whenComplete((value, error) -> {
            timeoutFuture.cancel(false);
            if (error == null) {
                view.complete(value);
            } else {
                view.completeExceptionally(error);
            }
        });
        return view;
    }

    private Duration leaseDuration(Duration timeout) {
        return timeout.compareTo(MINIMUM_LEASE_DURATION) < 0
                ? MINIMUM_LEASE_DURATION : timeout.plus(Duration.ofMinutes(1));
    }

    private <T> T runWithLease(String key, Duration timeout, Supplier<T> task, Duration leaseDuration) {
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (!lease.tryAcquire(key, leaseDuration)) {
                if (System.nanoTime() >= deadline) {
                    throw new java.util.concurrent.TimeoutException("conversion lease timeout");
                }
                Thread.sleep(Math.min(LEASE_POLL_MILLIS,
                        Math.max(MIN_SLEEP_MILLIS, Duration.ofNanos(deadline - System.nanoTime()).toMillis())));
            }
            try {
                return task.get();
            } finally {
                lease.release(key);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new CompletionException(interrupted);
        } catch (java.util.concurrent.TimeoutException timeoutException) {
            throw new CompletionException(timeoutException);
        }
    }

    private static ScheduledExecutorService defaultTimeoutScheduler() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "file-preview-conversion-timeout");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(factory);
    }

    private static Executor defaultConversionExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "file-preview-conversion-worker");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(DEFAULT_CONVERSION_WORKERS, factory);
    }

}
