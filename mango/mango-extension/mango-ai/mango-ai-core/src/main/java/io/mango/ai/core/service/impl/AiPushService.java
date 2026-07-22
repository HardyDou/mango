package io.mango.ai.core.service.impl;

import io.mango.ai.core.service.IAiPushService;
import io.mango.infra.realtime.api.RealtimeApi;

/**
 * AI 模块实时推送服务。
 */
public class AiPushService implements IAiPushService {

    private final RealtimeApi realtimeApi;

    public AiPushService(RealtimeApi realtimeApi) {
        this.realtimeApi = realtimeApi;
    }

    @Override
    public void broadcastNotification(String content) {
        realtimeApi.broadcast("notification", content);
    }

    @Override
    public void broadcastAlert(String content) {
        realtimeApi.broadcast("alert", content);
    }
}
