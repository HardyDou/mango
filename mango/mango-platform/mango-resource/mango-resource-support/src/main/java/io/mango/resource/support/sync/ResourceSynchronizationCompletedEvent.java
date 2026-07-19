package io.mango.resource.support.sync;

/**
 * Signals that a previously deferred resource synchronization has completed.
 */
public final class ResourceSynchronizationCompletedEvent {

    private final String applicationName;

    /**
     * Creates a completion event.
     *
     * @param applicationName application that synchronized its declarations
     */
    public ResourceSynchronizationCompletedEvent(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Returns the application that completed synchronization.
     *
     * @return application name, possibly empty when the application has no configured name
     */
    public String getApplicationName() {
        return applicationName;
    }
}
