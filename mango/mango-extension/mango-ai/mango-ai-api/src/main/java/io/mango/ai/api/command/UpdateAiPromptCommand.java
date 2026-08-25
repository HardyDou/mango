package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 更新 Prompt 模板。 */
@Getter
@Setter
public class UpdateAiPromptCommand extends CreateAiPromptCommand {
    @NotNull
    @Schema(description = "记录标识")
    private Long id;
}
