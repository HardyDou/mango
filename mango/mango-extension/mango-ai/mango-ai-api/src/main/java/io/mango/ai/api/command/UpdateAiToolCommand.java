package io.mango.ai.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 更新 AI 工具。 */
@Getter
@Setter
public class UpdateAiToolCommand extends CreateAiToolCommand {
    @NotNull
    private Long id;
}
