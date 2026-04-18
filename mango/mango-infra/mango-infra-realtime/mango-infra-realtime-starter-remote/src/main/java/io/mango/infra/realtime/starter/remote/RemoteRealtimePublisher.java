package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimePublisher;
import lombok.RequiredArgsConstructor;

/**
 * Realtime publisher implementation backed by a remote RealtimeApi.
 */
@RequiredArgsConstructor
public class RemoteRealtimePublisher implements RealtimePublisher {

    private final RealtimeApi realtimeApi;

    @Override
    public void publish(RealtimeMessage envelope) {
        realtimeApi.publish(envelope);
    }
}
