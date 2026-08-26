package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 更新 Skill。 */
@Getter
@Setter
public class UpdateAiSkillCommand extends CreateAiSkillCommand {
    @NotNull
    @Schema(description = "记录标识")
    private Long id;
}
