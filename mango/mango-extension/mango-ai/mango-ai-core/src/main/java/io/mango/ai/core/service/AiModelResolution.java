package io.mango.ai.core.service;

import io.mango.ai.api.enums.AiApiProtocol;
import io.mango.ai.api.enums.AiModality;
import lombok.Getter;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Set;

/** 已按租户能力路由解析出的可执行 ChatModel。 */
@Getter
public final class AiModelResolution {
    private final Long modelId;
    private final ChatModel chatModel;
    private final String providerCode;
    private final String modelName;
    private final AiApiProtocol apiProtocol;
    private final boolean thinkingConfigurable;
    private final Set<AiModality> inputModalities;
    private final Set<AiModality> outputModalities;

    public AiModelResolution(
            Long modelId,
            ChatModel chatModel,
            String providerCode,
            String modelName,
            AiApiProtocol apiProtocol,
            boolean thinkingConfigurable,
            Set<AiModality> inputModalities,
            Set<AiModality> outputModalities) {
        this.modelId = modelId;
        this.chatModel = chatModel;
        this.providerCode = providerCode;
        this.modelName = modelName;
        this.apiProtocol = apiProtocol;
        this.thinkingConfigurable = thinkingConfigurable;
        this.inputModalities = Set.copyOf(inputModalities);
        this.outputModalities = Set.copyOf(outputModalities);
    }
}
