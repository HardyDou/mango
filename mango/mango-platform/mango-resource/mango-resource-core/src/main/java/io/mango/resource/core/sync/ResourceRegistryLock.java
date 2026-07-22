package io.mango.resource.core.sync;

import io.mango.infra.kv.api.ILeaseLocker;
import io.mango.infra.kv.api.LockLease;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owner-safe renewable lease for Resource Registry synchronization.
 */
@Slf4j
public class ResourceRegistryLock implements AutoCloseable {

    public static final String LOCK_NAME = "mango-resource-sync";
    private static final long LEASE_RENEWAL_DIVISOR = 3L;

    private final ILeaseLocker locker;
    private final ScheduledExecutorService renewalExecutor;
    private final AtomicReference<LeaseSession> activeSession = new AtomicReference<>();
    private boolean closed;

    public ResourceRegistryLock(ILeaseLocker locker) {
        this.locker = locker;
        this.renewalExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mango-resource-lease-renewal");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Attempts to acquire and automatically renew the Resource Registry lease.
     *
     * @param owner observable process or attempt owner
     * @param ttlSeconds positive lease TTL
     * @return active session, or empty when another owner holds the lease
     */
    public synchronized Optional<LeaseSession> tryLock(String owner, int ttlSeconds) {
        if (closed) {
            throw new IllegalStateException("Resource Registry lock is closed");
        }
        Optional<LockLease> acquired = locker.tryAcquire(LOCK_NAME, owner, ttlSeconds);
        if (acquired.isEmpty()) {
            return Optional.empty();
        }
        LeaseSession session = new LeaseSession(acquired.get(), ttlSeconds);
        if (!activeSession.compareAndSet(null, session)) {
            locker.release(acquired.get());
            throw new IllegalStateException("Resource Registry already has an active local lease session");
        }
        session.start();
        return Optional.of(session);
    }

    /**
     * Fails fast when the active synchronization no longer owns its lease.
     */
    public void assertOwned() {
        LeaseSession session = activeSession.get();
        if (session == null || !session.isOwned()) {
            throw new IllegalStateException("Resource Registry lease ownership was lost");
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        LeaseSession session = activeSession.get();
        if (session != null) {
            session.close();
        }
        renewalExecutor.shutdownNow();
    }

    /**
     * Active lease session owned by one synchronization attempt.
     */
    public final class LeaseSession implements AutoCloseable {

        private final int ttlSeconds;
        private LockLease lease;
        private boolean owned = true;
        private ScheduledFuture<?> renewalTask;

        private LeaseSession(LockLease lease, int ttlSeconds) {
            this.lease = lease;
            this.ttlSeconds = ttlSeconds;
        }

        private synchronized void start() {
            long renewalInterval = Math.max(1L, ttlSeconds / LEASE_RENEWAL_DIVISOR);
            renewalTask = renewalExecutor.scheduleWithFixedDelay(
                    this::renewSafely, renewalInterval, renewalInterval, TimeUnit.SECONDS);
        }

        private void renewSafely() {
            synchronized (this) {
                if (!owned) {
                    return;
                }
                try {
                    Optional<LockLease> renewed = locker.renew(lease, ttlSeconds);
                    if (renewed.isPresent()) {
                        lease = renewed.get();
                        return;
                    }
                    owned = false;
                    log.error("Mango resource registry lease lost: owner={}", lease.owner());
                } catch (RuntimeException exception) {
                    owned = false;
                    log.error("Mango resource registry lease renewal failed: owner={}", lease.owner(), exception);
                }
            }
        }

        /**
         * Returns whether this attempt still owns the current live lease.
         */
        public synchronized boolean isOwned() {
            return owned;
        }

        @Override
        public void close() {
            synchronized (this) {
                if (renewalTask != null) {
                    renewalTask.cancel(false);
                }
                if (owned) {
                    try {
                        if (!locker.release(lease)) {
                            log.warn("Mango resource registry lease release skipped: owner no longer current, owner={}",
                                    lease.owner());
                        }
                    } catch (RuntimeException exception) {
                        log.error("Mango resource registry lease release failed: owner={}", lease.owner(), exception);
                    }
                }
                owned = false;
            }
            activeSession.compareAndSet(this, null);
        }
    }
}
