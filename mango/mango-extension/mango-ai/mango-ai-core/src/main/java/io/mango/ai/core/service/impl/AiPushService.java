package io.mango.ai.core.service.impl;

import io.mango.ai.core.service.IAiPushService;
import io.mango.infra.realtime.api.RealtimeApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * AI 模块实时推送服务。
 */
@Service
@ConditionalOnBean(RealtimeApi.class)
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
