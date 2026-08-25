package io.mango.ai.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 更新 AI 服务定义。 */
@Getter
@Setter
public class UpdateAiServiceCommand extends CreateAiServiceCommand {
    @NotNull
    private Long id;
}
