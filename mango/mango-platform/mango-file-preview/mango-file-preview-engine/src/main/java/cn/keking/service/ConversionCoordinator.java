package cn.keking.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 合并同一转换键的并发请求，并保证超时后可以重新提交。 */
@Component
public final class ConversionCoordinator {
    private final ConcurrentMap<String, CompletableFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Executor executor;
    private final ConversionTaskLease lease;

    public ConversionCoordinator() { this(ForkJoinPool.commonPool(), new LocalConversionTaskLease()); }

    @Autowired
    public ConversionCoordinator(ConversionTaskLease lease) { this(ForkJoinPool.commonPool(), lease); }

    ConversionCoordinator(Executor executor) { this(executor, new LocalConversionTaskLease()); }

    ConversionCoordinator(Executor executor, ConversionTaskLease lease) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.lease = Objects.requireNonNull(lease, "lease");
    }

    public <T> CompletableFuture<T> submit(String key, Duration timeout, Supplier<T> task) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(task, "task");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        @SuppressWarnings("unchecked")
        CompletableFuture<T> result = (CompletableFuture<T>) tasks.computeIfAbsent(key, ignored -> {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> runWithLease(key, timeout, task), executor)
                    .orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            future.whenComplete((value, error) -> tasks.remove(key, future));
            return future;
        });
        return result;
    }

    public int inFlight() { return tasks.size(); }

    private <T> T runWithLease(String key, Duration timeout, Supplier<T> task) {
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (!lease.tryAcquire(key, timeout)) {
                if (System.nanoTime() >= deadline) throw new java.util.concurrent.TimeoutException("conversion lease timeout");
                Thread.sleep(Math.min(50L, Math.max(1L, Duration.ofNanos(deadline - System.nanoTime()).toMillis())));
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
}
