package io.mango.infra.sensitive.api;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Thread-local switch for output masking.
 *
 * <p>This is intended for internal serialization paths that must explicitly
 * emit raw values. The scope is limited to the current thread and should be
 * used with try-with-resources.</p>
 */
@LocalCapabilityContract
public final class SensitiveMaskingContext {

    private static final ThreadLocal<Integer> DISABLE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private SensitiveMaskingContext() {
    }

    /**
     * Disables masking for the current thread until the returned scope is closed.
     *
     * @return closeable scope
     */
    public static Scope disable() {
        DISABLE_DEPTH.set(DISABLE_DEPTH.get() + 1);
        return new MaskingScope();
    }

    /**
     * Runs a supplier with masking disabled for the current thread.
     *
     * @param supplier supplier to execute
     * @param <T>      result type
     * @return supplier result
     */
    public static <T> T getWithoutMasking(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        try (Scope ignored = disable()) {
            return supplier.get();
        }
    }

    /**
     * Runs an action with masking disabled for the current thread.
     *
     * @param action action to execute
     */
    public static void runWithoutMasking(Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        try (Scope ignored = disable()) {
            action.run();
        }
    }

    /**
     * Returns whether masking is disabled in the current thread.
     *
     * @return true when masking is disabled
     */
    public static boolean isMaskingDisabled() {
        return DISABLE_DEPTH.get() > 0;
    }

    private static void enableOneLevel() {
        int depth = DISABLE_DEPTH.get() - 1;
        if (depth <= 0) {
            DISABLE_DEPTH.remove();
            return;
        }
        DISABLE_DEPTH.set(depth);
    }

    @LocalCapabilityContract
    private static final class MaskingScope implements Scope {

        private final Thread owner = Thread.currentThread();

        private boolean closed;

        @Override
        public void close() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("masking scope must be closed by its owning thread");
            }
            if (closed) {
                return;
            }
            closed = true;
            enableOneLevel();
        }
    }

    /**
     * Closeable masking scope.
     */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {

        @Override
        void close();
    }
}
