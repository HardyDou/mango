package io.mango.infra.module.core.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticReport;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import io.mango.infra.module.api.diagnostic.ModuleRuntimeStatus;
import io.mango.infra.module.api.diagnostic.ModuleVersionEvidence;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Bounded single-flight execution for runtime diagnostics.
 * Expired successful evidence is never returned when a refresh fails.
 */
public class ModuleDiagnosticCoordinator implements AutoCloseable {

    private static final int DEFAULT_THREADS = 4;
    private static final int DEFAULT_QUEUE_CAPACITY = 16;
    private static final int DEFAULT_MAX_KEYS = 64;
    private static final int THREAD_KEEP_ALIVE_SECONDS = 30;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofSeconds(2);

    private final ModuleDiagnosticAggregator aggregator;
    private final ThreadPoolExecutor executor;
    private final Duration timeout;
    private final Duration cacheTtl;
    private final int maxKeys;
    private final DiagnosticAuditSink auditSink;
    private final Semaphore admissionPermits;
    private final Semaphore flightPermits;
    private final Map<RequestKey, Flight> inFlight = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object cacheLock = new Object();
    private final Map<RequestKey, CachedReport> cache = new LinkedHashMap<>(16, 0.75f, true);

    public ModuleDiagnosticCoordinator(ModuleDiagnosticAggregator aggregator) {
        this(aggregator, DEFAULT_THREADS, DEFAULT_QUEUE_CAPACITY, DEFAULT_MAX_KEYS,
                DEFAULT_TIMEOUT, DEFAULT_CACHE_TTL);
    }

    public ModuleDiagnosticCoordinator(
            ModuleDiagnosticAggregator aggregator,
            int threads,
            int queueCapacity,
            int maxKeys,
            Duration timeout,
            Duration cacheTtl) {
        this(aggregator, threads, queueCapacity, maxKeys, timeout, cacheTtl, systemAuditSink());
    }

    ModuleDiagnosticCoordinator(
            ModuleDiagnosticAggregator aggregator,
            int threads,
            int queueCapacity,
            int maxKeys,
            Duration timeout,
            Duration cacheTtl,
            DiagnosticAuditSink auditSink) {
        this.aggregator = aggregator;
        this.timeout = requirePositive(timeout, "timeout");
        this.cacheTtl = requirePositive(cacheTtl, "cacheTtl");
        if (threads <= 0 || queueCapacity <= 0 || maxKeys <= 0) {
            throw new IllegalArgumentException("threads, queueCapacity and maxKeys must be positive");
        }
        this.maxKeys = maxKeys;
        this.auditSink = auditSink == null ? ignored -> { } : auditSink;
        this.admissionPermits = new Semaphore(maxKeys, true);
        this.flightPermits = new Semaphore(maxKeys, true);
        this.executor = new ThreadPoolExecutor(
                threads,
                threads,
                THREAD_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                runnable -> {
                    // Diagnostic work must not inherit a caller's security, tenant or token ThreadLocals.
                    Thread thread = new Thread(
                            null, runnable, "mango-module-diagnostic", 0, false);
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
        this.executor.allowCoreThreadTimeOut(true);
    }

    public ModuleDiagnosticReport diagnose(ModuleDiagnosticRequest request) {
        long invocationStartedNanos = System.nanoTime();
        String requestId = auditRequestId(request);
        RequestKey key = RequestKey.from(request);
        CachedReport cached = currentCached(key);
        if (cached != null) {
            return cached.report();
        }
        if (!admissionPermits.tryAcquire()) {
            auditUnavailable(request, requestId, "DIAGNOSTIC_OVERLOADED", invocationStartedNanos);
            return unavailable(request, "DIAGNOSTIC_OVERLOADED");
        }

        try {
            CachedReport refreshedBeforeFlight = currentCached(key);
            if (refreshedBeforeFlight != null) {
                return refreshedBeforeFlight.report();
            }
            Flight flight = inFlight.get(key);
            if (flight == null) {
                Flight candidate = new Flight(System.nanoTime());
                Flight existing = inFlight.putIfAbsent(key, candidate);
                flight = existing == null ? candidate : existing;
                if (existing == null) {
                    if (!flightPermits.tryAcquire()) {
                        candidate.fail(new FlightCancellation("DIAGNOSTIC_OVERLOADED"));
                        inFlight.remove(key, candidate);
                        auditUnavailable(request, requestId, "DIAGNOSTIC_OVERLOADED", invocationStartedNanos);
                        return unavailable(request, "DIAGNOSTIC_OVERLOADED");
                    }
                    CachedReport refreshed = currentCached(key);
                    if (refreshed != null) {
                        candidate.publish(refreshed.report(), () -> { });
                        removeFlight(key, candidate);
                        return refreshed.report();
                    }
                    submit(key, request, candidate);
                }
            }
            return await(request, key, flight, requestId, invocationStartedNanos);
        } finally {
            admissionPermits.release();
        }
    }

    private ModuleDiagnosticReport await(
            ModuleDiagnosticRequest request,
            RequestKey key,
            Flight flight,
            String requestId,
            long invocationStartedNanos) {
        long remainingNanos = timeout.toNanos() - Math.max(0, System.nanoTime() - flight.createdNanos());
        if (remainingNanos <= 0) {
            cancelTimedOutFlight(key, flight);
            auditUnavailable(request, requestId, "DIAGNOSTIC_TIMEOUT", invocationStartedNanos);
            return unavailable(request, "DIAGNOSTIC_TIMEOUT");
        }
        try {
            return flight.result().get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            cancelTimedOutFlight(key, flight);
            auditUnavailable(request, requestId, "DIAGNOSTIC_TIMEOUT", invocationStartedNanos);
            return unavailable(request, "DIAGNOSTIC_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            auditUnavailable(request, requestId, "DIAGNOSTIC_INTERRUPTED", invocationStartedNanos);
            return unavailable(request, "DIAGNOSTIC_INTERRUPTED");
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof FlightCancellation cancellation) {
                auditUnavailable(request, requestId, cancellation.reasonCode(), invocationStartedNanos);
                return unavailable(request, cancellation.reasonCode());
            }
            auditUnavailable(request, requestId, "DIAGNOSTIC_OVERLOADED", invocationStartedNanos);
            return unavailable(request, "DIAGNOSTIC_OVERLOADED");
        } catch (RuntimeException exception) {
            auditUnavailable(request, requestId, "DIAGNOSTIC_OVERLOADED", invocationStartedNanos);
            return unavailable(request, "DIAGNOSTIC_OVERLOADED");
        }
    }

    private void cancelTimedOutFlight(RequestKey key, Flight flight) {
        if (flight.cancel("DIAGNOSTIC_TIMEOUT", executor)) {
            // A queued task removed from the executor will never enter its finally block.
            flight.finish();
            removeFlight(key, flight);
        }
    }

    private void submit(RequestKey key, ModuleDiagnosticRequest request, Flight flight) {
        try {
            Future<?> task = executor.submit(() -> execute(key, request, flight));
            if (flight.bindTask(task, executor)) {
                flight.finish();
                removeFlight(key, flight);
            }
        } catch (RuntimeException exception) {
            flight.fail(exception);
            flight.finish();
            removeFlight(key, flight);
        }
    }

    private void execute(RequestKey key, ModuleDiagnosticRequest request, Flight flight) {
        try {
            if (!flight.start()) {
                return;
            }
            ModuleDiagnosticReport report = aggregator.diagnose(request);
            flight.publish(report, () -> cacheReport(key, report));
        } catch (RuntimeException exception) {
            flight.fail(exception);
        } finally {
            flight.finish();
            removeFlight(key, flight);
            // Do not let an interrupted diagnostic poison a reused worker thread.
            Thread.interrupted();
        }
    }

    private CachedReport currentCached(RequestKey key) {
        synchronized (cacheLock) {
            Instant now = Instant.now();
            CachedReport cached = cache.get(key);
            if (cached == null) {
                return null;
            }
            if (cached.expiresAt().isAfter(now)) {
                return cached;
            }
            cache.remove(key);
            return null;
        }
    }

    private void cacheReport(RequestKey key, ModuleDiagnosticReport report) {
        synchronized (cacheLock) {
            cache.put(key, new CachedReport(report, Instant.now().plus(cacheTtl)));
            while (cache.size() > maxKeys) {
                var oldest = cache.entrySet().iterator();
                oldest.next();
                oldest.remove();
            }
        }
    }

    int cachedReportCount() {
        synchronized (cacheLock) {
            return cache.size();
        }
    }

    private void removeFlight(RequestKey key, Flight flight) {
        if (inFlight.remove(key, flight)) {
            flightPermits.release();
        }
    }

    private ModuleDiagnosticReport unavailable(ModuleDiagnosticRequest request, String reasonCode) {
        Instant observedAt = Instant.now();
        List<ModuleDiagnosticCondition> conditions = request.profile().requiredConditionIds().stream()
                .sorted()
                .map(conditionId -> new ModuleDiagnosticCondition(
                        conditionId,
                        ModuleConditionStatus.UNKNOWN,
                        true,
                        reasonCode,
                        Map.of(),
                        observedAt,
                        timeout.toMillis(),
                        false))
                .toList();
        ModuleVersionEvidence unknown = new ModuleVersionEvidence(
                null, "NONE", ModuleConditionStatus.UNKNOWN, "VERSION_UNKNOWN");
        return new ModuleDiagnosticReport(
                request.moduleCode(),
                ModuleRuntimeStatus.UNKNOWN,
                false,
                unknown,
                unknown,
                new ModuleVersionEvidence(
                        null, "NONE", ModuleConditionStatus.UNKNOWN, "NO_EXPECTATION_PROVIDER"),
                conditions);
    }

    private void auditUnavailable(
            ModuleDiagnosticRequest request,
            String requestId,
            String reasonCode,
            long invocationStartedNanos) {
        try {
            auditSink.record(new DiagnosticAuditEvent(
                    safeAuditText(request.moduleCode(), 80),
                    safeAuditText(request.appCode(), 80),
                    ModuleRuntimeStatus.UNKNOWN.name(),
                    safeAuditText(reasonCode, 80),
                    Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - invocationStartedNanos)),
                    requestId));
        } catch (RuntimeException ignored) {
            // Diagnostics and their audit trail are best effort and must not affect application availability.
        }
    }

    @Override
    public void close() {
        inFlight.forEach((key, flight) -> cancelTimedOutFlight(key, flight));
        executor.shutdownNow();
        synchronized (cacheLock) {
            cache.clear();
        }
        inFlight.clear();
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static String auditRequestId(ModuleDiagnosticRequest request) {
        String supplied = request.attributes().get("requestId");
        if (supplied != null && !supplied.isBlank()) {
            return safeAuditText(supplied, 64);
        }
        return UUID.randomUUID().toString();
    }

    private static String safeAuditText(String value, int maxLength) {
        String normalized = value == null ? "unknown" : value.trim();
        if (normalized.isEmpty()) {
            return "unknown";
        }
        String safe = normalized.replaceAll("[^A-Za-z0-9._:-]", "_");
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private static DiagnosticAuditSink systemAuditSink() {
        System.Logger logger = System.getLogger(ModuleDiagnosticCoordinator.class.getName());
        return event -> logger.log(
                System.Logger.Level.WARNING,
                "module diagnostic unavailable: module={0}, app={1}, status={2}, reason={3}, durationMs={4}, requestId={5}",
                event.moduleCode(),
                event.appCode(),
                event.status(),
                event.reasonCode(),
                event.durationMs(),
                event.requestId());
    }

    private record RequestKey(String moduleCode, String appCode, String profile, Map<String, String> attributes) {
        private static RequestKey from(ModuleDiagnosticRequest request) {
            return new RequestKey(
                    request.moduleCode(),
                    request.appCode(),
                    request.profile().name(),
                    Map.copyOf(request.attributes()));
        }
    }

    private record CachedReport(ModuleDiagnosticReport report, Instant expiresAt) {
    }

    record DiagnosticAuditEvent(
            String moduleCode,
            String appCode,
            String status,
            String reasonCode,
            long durationMs,
            String requestId) {
    }

    @FunctionalInterface
    interface DiagnosticAuditSink {
        void record(DiagnosticAuditEvent event);
    }

    private static final class Flight {
        private final CompletableFuture<ModuleDiagnosticReport> result = new CompletableFuture<>();
        private final long createdNanos;
        private Future<?> task;
        private boolean started;
        private boolean cancellationRequested;
        private boolean finished;

        private Flight(long createdNanos) {
            this.createdNanos = createdNanos;
        }

        private long createdNanos() {
            return createdNanos;
        }

        private CompletableFuture<ModuleDiagnosticReport> result() {
            return result;
        }

        /**
         * @return true when a cancelled queued task was physically removed and will never run
         */
        private synchronized boolean bindTask(Future<?> submittedTask, ThreadPoolExecutor executor) {
            task = submittedTask;
            if (!cancellationRequested) {
                return false;
            }
            submittedTask.cancel(true);
            return submittedTask instanceof Runnable runnable && executor.remove(runnable);
        }

        private synchronized boolean start() {
            started = true;
            return !cancellationRequested && !finished;
        }

        private synchronized boolean publish(ModuleDiagnosticReport report, Runnable beforeComplete) {
            if (cancellationRequested || finished) {
                return false;
            }
            beforeComplete.run();
            result.complete(report);
            return true;
        }

        private synchronized void fail(Throwable failure) {
            if (!cancellationRequested && !finished) {
                result.completeExceptionally(failure);
            }
        }

        /**
         * Cancels the actual executor future. Running work stays registered until its finally block executes.
         *
         * @return true only when a queued task was removed and requires caller-side cleanup
         */
        private synchronized boolean cancel(String reasonCode, ThreadPoolExecutor executor) {
            if (cancellationRequested || finished) {
                return false;
            }
            cancellationRequested = true;
            result.completeExceptionally(new FlightCancellation(reasonCode));
            if (task == null) {
                return false;
            }
            task.cancel(true);
            return !started && task instanceof Runnable runnable && executor.remove(runnable);
        }

        private synchronized void finish() {
            finished = true;
        }
    }

    private static final class FlightCancellation extends RuntimeException {
        private final String reasonCode;

        private FlightCancellation(String reasonCode) {
            super(reasonCode, null, false, false);
            this.reasonCode = reasonCode;
        }

        private String reasonCode() {
            return reasonCode;
        }
    }
}
