package io.mango.resource.support.sync;

import java.util.Objects;

/**
 * Signals that a startup readiness participant changed state.
 *
 * @param component participant name
 * @param state current state
 */
public record StartupReadinessChangedEvent(String component, StartupReadinessState state) {

    public StartupReadinessChangedEvent {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(state, "state");
    }
}
