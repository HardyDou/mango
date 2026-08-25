package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "AI 能力类型")
    private AiCapability capability;
    @NotNull @Positive
    @Schema(description = "模型标识")
    private Long modelId;
}
