package io.mango.infra.realtime.e2e.apps.local.listener;

import io.mango.infra.realtime.api.annotation.RealtimeInboundMessageListener;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class LocalPrimaryInboundListener {

    public static final BlockingQueue<String> EVENTS = new LinkedBlockingQueue<>();

    @RealtimeInboundMessageListener(types = "task.cancel")
    public void onMessage(RealtimeInboundMessage message) {
        EVENTS.offer("local-primary:" + message.content());
    }

    public static void reset() {
        EVENTS.clear();
    }
}
