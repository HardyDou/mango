package io.mango.resource.support.sync;

import java.util.Map;

/**
 * A startup participant whose state contributes to application readiness.
 */
public interface StartupReadinessStatus {

    /**
     * Returns the stable participant name used in health details.
     *
     * @return participant name
     */
    String getReadinessComponent();

    /**
     * Returns the participant's current startup state.
     *
     * @return current state
     */
    StartupReadinessState getReadinessState();

    /**
     * Returns whether this participant has converged.
     *
     * @return {@code true} only when the participant is ready
     */
    default boolean isReady() {
        return getReadinessState() == StartupReadinessState.READY;
    }

    /**
     * Returns non-sensitive operational details for readiness diagnostics.
     *
     * @return diagnostic details
     */
    default Map<String, Object> getReadinessDetails() {
        return Map.of("state", getReadinessState().name());
    }
}
