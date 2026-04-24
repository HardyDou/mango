package io.mango.infra.realtime.e2e.apps.local.listener.extra;

import io.mango.infra.realtime.api.annotation.RealtimeInboundMessageListener;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class LocalExtraInboundListener {

    public static final BlockingQueue<String> EVENTS = new LinkedBlockingQueue<>();

    @RealtimeInboundMessageListener(types = "task.cancel")
    public void onMessage(RealtimeInboundMessage message) {
        EVENTS.offer("local-extra:" + message.content());
    }

    public static void reset() {
        EVENTS.clear();
    }
}
