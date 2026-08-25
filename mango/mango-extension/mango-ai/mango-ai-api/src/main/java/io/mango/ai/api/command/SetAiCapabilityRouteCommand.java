package io.mango.ai.api.command;

import io.mango.ai.api.enums.AiCapability;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** 设置租户某项 AI 能力的默认模型。 */
@Getter
@Setter
public class SetAiCapabilityRouteCommand {
    @NotNull
    private AiCapability capability;
    @NotNull @Positive
    private Long modelId;
}
