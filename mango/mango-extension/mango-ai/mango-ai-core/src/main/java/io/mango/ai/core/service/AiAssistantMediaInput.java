package io.mango.ai.core.service;

import org.springframework.ai.content.Media;

/** 保存一项模型输出媒体所需的调用信息。 */
public record AiAssistantMediaInput(Media media, String requestId, int index) {
}
