package io.mango.infra.module.core.diagnostic;

import io.mango.infra.module.api.diagnostic.ModuleConditionStatus;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticCondition;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticContributor;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticProfile;
import io.mango.infra.module.api.diagnostic.ModuleDiagnosticRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleDiagnosticCoordinatorTest {

    @Test
    void concurrentSameKeyUsesOneContributorExecution() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch contributorStarted = new CountDownLatch(1);
        CountDownLatch releaseContributor = new CountDownLatch(1);
        ModuleDiagnosticContributor contributor = request -> {
            executions.incrementAndGet();
            contributorStarted.countDown();
            await(releaseContributor);
            return List.of(pass());
        };
        try (ModuleDiagnosticCoordinator coordinator = coordinator(contributor, Duration.ofSeconds(2));
             var callers = Executors.newFixedThreadPool(32)) {
            CountDownLatch ready = new CountDownLatch(32);
            CountDownLatch start = new CountDownLatch(1);
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
            for (int index = 0; index < 32; index++) {
                futures.add(callers.submit(() -> {
                    ready.countDown();
                    await(start);
                    coordinator.diagnose(request());
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(contributorStarted.await(5, TimeUnit.SECONDS));
            releaseContributor.countDown();
            for (var future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        }
        assertEquals(1, executions.get());
    }

    @Test
    void callersBeyondAdmissionLimitFailFastEvenForTheSameKey() throws Exception {
        CountDownLatch contributorStarted = new CountDownLatch(1);
        CountDownLatch releaseContributor = new CountDownLatch(1);
        ModuleDiagnosticContributor contributor = request -> {
            contributorStarted.countDown();
            await(releaseContributor);
            return List.of(pass());
        };
        try (ModuleDiagnosticCoordinator coordinator = new ModuleDiagnosticCoordinator(
                new ModuleDiagnosticAggregator(List.of(contributor)),
                1,
                1,
                1,
                Duration.ofSeconds(2),
                Duration.ofSeconds(1));
             var caller = Executors.newSingleThreadExecutor()) {
            var first = caller.submit(() -> coordinator.diagnose(request()));
            assertTrue(contributorStarted.await(1, TimeUnit.SECONDS));

            long startedNanos = System.nanoTime();
            var overloaded = coordinator.diagnose(request());
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);

            assertEquals("UNKNOWN", overloaded.status().name());
            assertTrue(overloaded.conditions().stream()
                    .allMatch(condition -> "DIAGNOSTIC_OVERLOADED".equals(condition.reasonCode())));
            assertTrue(elapsedMillis < 250, "overload response should fail fast");

            releaseContributor.countDown();
            assertEquals("READY", first.get(1, TimeUnit.SECONDS).status().name());
        } finally {
            releaseContributor.countDown();
        }
    }

    @Test
    void cacheCapacityRemainsStrictDuringConcurrentMultiKeyRefresh() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(4);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        ModuleDiagnosticContributor contributor = request -> {
            if (request.attributes().getOrDefault("cacheKey", "").startsWith("refresh-")) {
                refreshStarted.countDown();
                await(releaseRefresh);
            }
            return List.of(pass());
        };
        try (ModuleDiagnosticCoordinator coordinator = new ModuleDiagnosticCoordinator(
                new ModuleDiagnosticAggregator(List.of(contributor)),
                4,
                4,
                4,
                Duration.ofSeconds(2),
                Duration.ofSeconds(5));
             var callers = Executors.newFixedThreadPool(4)) {
            for (int index = 0; index < 4; index++) {
                assertEquals("READY", coordinator.diagnose(request("warm-" + index)).status().name());
            }
            assertEquals(4, coordinator.cachedReportCount());

            List<java.util.concurrent.Future<?>> refreshes = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                int cacheKey = index;
                refreshes.add(callers.submit(() -> coordinator.diagnose(request("refresh-" + cacheKey))));
            }
            assertTrue(refreshStarted.await(1, TimeUnit.SECONDS));
            releaseRefresh.countDown();
            for (var refresh : refreshes) {
                refresh.get(1, TimeUnit.SECONDS);
            }
            assertEquals(4, coordinator.cachedReportCount());
        } finally {
            releaseRefresh.countDown();
        }
    }

    @Test
    void timeoutReturnsUnknownInsteadOfOldPass() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        ModuleDiagnosticContributor contributor = request -> {
            if (executions.incrementAndGet() > 1) {
                sleep(200);
            }
            return List.of(pass());
        };
        try (ModuleDiagnosticCoordinator coordinator = new ModuleDiagnosticCoordinator(
                new ModuleDiagnosticAggregator(List.of(contributor)),
                1,
                1,
                4,
                Duration.ofMillis(30),
                Duration.ofMillis(5))) {
            assertEquals("READY", coordinator.diagnose(request()).status().name());
            Thread.sleep(10);
            assertEquals("UNKNOWN", coordinator.diagnose(request()).status().name());
        }
    }

    @Test
    void timeoutInterruptsActualTaskWithoutStartingDuplicateFlight() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        List<ModuleDiagnosticCoordinator.DiagnosticAuditEvent> auditEvents =
                new java.util.concurrent.CopyOnWriteArrayList<>();
        ModuleDiagnosticContributor contributor = request -> {
            int execution = executions.incrementAndGet();
            if (execution == 1) {
                started.countDown();
                try {
                    while (!release.await(1, TimeUnit.SECONDS)) {
                        // Keep the first physical flight alive until the test releases it.
                    }
                } catch (InterruptedException exception) {
                    interruptions.incrementAndGet();
                    while (release.getCount() > 0) {
                        Thread.onSpinWait();
                    }
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            }
            return List.of(pass());
        };
        try (ModuleDiagnosticCoordinator coordinator = new ModuleDiagnosticCoordinator(
                new ModuleDiagnosticAggregator(List.of(contributor)),
                1,
                1,
                4,
                Duration.ofMillis(40),
                Duration.ofSeconds(1),
                auditEvents::add)) {
            ModuleDiagnosticRequest auditedRequest = new ModuleDiagnosticRequest(
                    "mango-link",
                    "internal-admin",
                    ModuleDiagnosticProfile.INSTALLATION_ONLY,
                    Map.of("requestId", "req-348"));
            assertEquals("UNKNOWN", coordinator.diagnose(auditedRequest).status().name());
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertEquals("UNKNOWN", coordinator.diagnose(auditedRequest).status().name());
            assertEquals(1, executions.get());
            assertEquals(1, interruptions.get());
            assertFalse(auditEvents.isEmpty());
            assertTrue(auditEvents.stream().allMatch(event ->
                    "mango-link".equals(event.moduleCode())
                            && "internal-admin".equals(event.appCode())
                            && "UNKNOWN".equals(event.status())
                            && "DIAGNOSTIC_TIMEOUT".equals(event.reasonCode())
                            && "req-348".equals(event.requestId())
                            && event.durationMs() >= 0));

            release.countDown();
            assertTrue(finished.await(1, TimeUnit.SECONDS));
            assertEquals("READY", awaitReady(coordinator, auditedRequest));
            assertEquals(2, executions.get());
        } finally {
            release.countDown();
        }
    }

    @Test
    void diagnosticWorkerDoesNotInheritCallerThreadLocals() {
        InheritableThreadLocal<String> callerContext = new InheritableThreadLocal<>();
        callerContext.set("secret-caller-context");
        java.util.concurrent.atomic.AtomicReference<String> observed = new java.util.concurrent.atomic.AtomicReference<>();
        ModuleDiagnosticContributor contributor = request -> {
            observed.set(callerContext.get());
            return List.of(pass());
        };
        try (ModuleDiagnosticCoordinator coordinator = coordinator(contributor, Duration.ofSeconds(1))) {
            assertEquals("READY", coordinator.diagnose(request()).status().name());
            assertNull(observed.get());
        } finally {
            callerContext.remove();
        }
    }

    private ModuleDiagnosticCoordinator coordinator(
            ModuleDiagnosticContributor contributor,
            Duration timeout) {
        return new ModuleDiagnosticCoordinator(
                new ModuleDiagnosticAggregator(List.of(contributor)),
                2,
                4,
                32,
                timeout,
                Duration.ofSeconds(1));
    }

    private ModuleDiagnosticRequest request() {
        return request(null);
    }

    private ModuleDiagnosticRequest request(String cacheKey) {
        return new ModuleDiagnosticRequest(
                "mango-link",
                "internal-admin",
                ModuleDiagnosticProfile.INSTALLATION_ONLY,
                cacheKey == null ? Map.of() : Map.of("cacheKey", cacheKey));
    }

    private String awaitReady(ModuleDiagnosticCoordinator coordinator, ModuleDiagnosticRequest request)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        String status;
        do {
            status = coordinator.diagnose(request).status().name();
            if ("READY".equals(status)) {
                return status;
            }
            Thread.sleep(5);
        } while (System.nanoTime() < deadline);
        return status;
    }

    private ModuleDiagnosticCondition pass() {
        return new ModuleDiagnosticCondition(
                ModuleDiagnosticProfile.INSTALLATION,
                ModuleConditionStatus.PASS,
                true,
                "INSTALLED",
                Map.of(),
                Instant.now(),
                0,
                false);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
