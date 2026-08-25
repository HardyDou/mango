package io.mango.ai.core.service;

/** 当前租户用户下的 AI 服务会话定位信息。 */
public record AiConversationScope(
        String tenantId,
        Long userId,
        String serviceCode,
        String sessionId) {
}
