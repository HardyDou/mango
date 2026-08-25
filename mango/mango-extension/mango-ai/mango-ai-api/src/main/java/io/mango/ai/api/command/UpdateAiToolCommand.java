package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 更新 AI 工具。 */
@Getter
@Setter
public class UpdateAiToolCommand extends CreateAiToolCommand {
    @NotNull
    @Schema(description = "记录标识")
    private Long id;
}
