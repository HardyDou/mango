package io.mango.infra.realtime.api;

/**
 * Internal API used by remote starters to register inbound receiver services.
 */
public interface RealtimeSubscriberApi {

    void register(RealtimeSubscriberRegistration registration);

    void unregister(RealtimeSubscriberRegistration registration);
}
