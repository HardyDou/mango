package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 更新 AI 服务定义。 */
@Getter
@Setter
public class UpdateAiServiceCommand extends CreateAiServiceCommand {
    @NotNull
    @Schema(description = "记录标识")
    private Long id;
}
