package io.mango.ai.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 创建 Prompt 模板。 */
@Getter
@Setter
public class CreateAiPromptCommand {
    @NotBlank
    @Size(max = 64)
    private String code;
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 500)
    private String description;
    @NotBlank
    @Size(max = 65535)
    private String template;
    @Size(max = 8192)
    private String variablesJson;
}
