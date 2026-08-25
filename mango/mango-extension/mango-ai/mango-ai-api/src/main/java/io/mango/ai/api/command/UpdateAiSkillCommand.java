package io.mango.ai.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 更新 Skill。 */
@Getter
@Setter
public class UpdateAiSkillCommand extends CreateAiSkillCommand {
    @NotNull
    private Long id;
}
