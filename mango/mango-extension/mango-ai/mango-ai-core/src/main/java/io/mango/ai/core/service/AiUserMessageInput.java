package io.mango.ai.core.service;

import io.mango.ai.api.vo.AiMessageContentPartVO;

import java.util.List;

/** 构造 Spring AI 用户消息所需的已归一化输入。 */
public record AiUserMessageInput(
        List<AiMessageContentPartVO> contentParts,
        AiModelResolution resolution,
        String modelText) {

    public AiUserMessageInput {
        contentParts = List.copyOf(contentParts);
    }
}
