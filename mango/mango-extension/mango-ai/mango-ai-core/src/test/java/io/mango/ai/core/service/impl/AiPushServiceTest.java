package io.mango.ai.core.service.impl;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI Realtime 推送行为测试。
 */
class AiPushServiceTest {

    @Test
    void broadcast_通知与告警委托给RealtimeApi() {
        RecordingRealtimeApi realtimeApi = new RecordingRealtimeApi();
        AiPushService service = new AiPushService(realtimeApi);

        service.broadcastNotification("notice");
        service.broadcastAlert("alert");

        assertEquals("notification", realtimeApi.type);
        assertEquals("alert", realtimeApi.lastType);
        assertEquals("notice", realtimeApi.content);
        assertEquals("alert", realtimeApi.lastContent);
    }

    private static final class RecordingRealtimeApi implements RealtimeApi {
        private String type;
        private String content;
        private String lastType;
        private String lastContent;

        @Override
        public void publish(RealtimeOutboundMessage message) {
            lastType = message.type();
            lastContent = message.content();
            if (type == null) {
                type = lastType;
                content = lastContent;
            }
        }
    }
}
