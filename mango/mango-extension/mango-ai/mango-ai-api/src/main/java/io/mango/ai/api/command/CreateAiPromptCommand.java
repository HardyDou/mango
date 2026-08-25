package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "配置编码")
    private String code;
    @NotBlank
    @Size(max = 100)
    @Schema(description = "显示名称")
    private String name;
    @Size(max = 500)
    @Schema(description = "业务说明")
    private String description;
    @NotBlank
    @Size(max = 65535)
    @Schema(description = "Prompt 模板正文")
    private String template;
    @Size(max = 8192)
    @Schema(description = "Prompt 变量定义 JSON")
    private String variablesJson;
}
