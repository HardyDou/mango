package io.mango.ai.core.service;

import io.mango.ai.api.dto.ChatRequest;
import reactor.core.publisher.Flux;

/**
 * AI 对话服务。
 */
public interface IChatService {

    /**
     * 发起流式对话。
     *
     * @param request 对话请求
     * @param tenantId 租户标识
     * @return 标准 JSON 事件流
     */
    Flux<String> chat(ChatRequest request, String tenantId);
}
