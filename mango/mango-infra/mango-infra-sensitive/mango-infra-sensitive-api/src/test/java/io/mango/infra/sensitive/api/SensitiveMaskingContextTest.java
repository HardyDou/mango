package io.mango.infra.sensitive.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensitiveMaskingContextTest {

    @Test
    void nestedScope_duplicateClose_doesNotReleaseOuterScope() {
        try (SensitiveMaskingContext.Scope outer = SensitiveMaskingContext.disable()) {
            SensitiveMaskingContext.Scope inner = SensitiveMaskingContext.disable();
            inner.close();
            inner.close();

            assertThat(SensitiveMaskingContext.isMaskingDisabled()).isTrue();
        }

        assertThat(SensitiveMaskingContext.isMaskingDisabled()).isFalse();
    }

    @Test
    void scopedOperations_restoreMaskingAfterFailure() {
        assertThatThrownBy(() -> SensitiveMaskingContext.getWithoutMasking(() -> {
            throw new IllegalStateException("controlled failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(SensitiveMaskingContext.isMaskingDisabled()).isFalse();
    }

    @Test
    void disabledScope_isIsolatedBetweenThreads() throws Exception {
        CountDownLatch scopeOpened = new CountDownLatch(1);
        CountDownLatch observationComplete = new CountDownLatch(1);
        AtomicBoolean otherThreadDisabled = new AtomicBoolean(true);

        Thread owner = Thread.ofPlatform().start(() -> {
            try (SensitiveMaskingContext.Scope ignored = SensitiveMaskingContext.disable()) {
                scopeOpened.countDown();
                await(observationComplete);
            }
        });
        Thread observer = Thread.ofPlatform().start(() -> {
            await(scopeOpened);
            otherThreadDisabled.set(SensitiveMaskingContext.isMaskingDisabled());
            observationComplete.countDown();
        });
        owner.join();
        observer.join();

        assertThat(otherThreadDisabled).isFalse();
        assertThat(SensitiveMaskingContext.isMaskingDisabled()).isFalse();
    }

    @Test
    void scopedOperations_rejectNullCallbacksWithoutChangingContext() {
        assertThatThrownBy(() -> SensitiveMaskingContext.getWithoutMasking(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("supplier must not be null");
        assertThatThrownBy(() -> SensitiveMaskingContext.runWithoutMasking(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("action must not be null");
        assertThat(SensitiveMaskingContext.isMaskingDisabled()).isFalse();
    }

    @Test
    void scope_closedByAnotherThread_rejectsCloseAndKeepsOwnerScopeActive() throws Exception {
        SensitiveMaskingContext.Scope scope = SensitiveMaskingContext.disable();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread otherThread = Thread.ofPlatform().start(() -> {
            try {
                scope.close();
            } catch (Throwable exception) {
                failure.set(exception);
            }
        });
        otherThread.join();

        try {
            assertThat(failure.get())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("masking scope must be closed by its owning thread");
            assertThat(SensitiveMaskingContext.isMaskingDisabled()).isTrue();
        } finally {
            scope.close();
        }
        assertThat(SensitiveMaskingContext.isMaskingDisabled()).isFalse();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("thread interrupted", exception);
        }
    }
}
