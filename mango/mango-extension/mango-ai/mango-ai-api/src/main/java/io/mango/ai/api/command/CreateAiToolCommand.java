package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 创建 AI 工具。 */
@Getter
@Setter
public class CreateAiToolCommand {
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
    @NotNull
    @Schema(description = "工具接入类型")
    private AiToolType toolType;
    @NotBlank
    @Size(max = 1024)
    @Schema(description = "工具调用端点")
    private String endpoint;
    @NotBlank
    @Size(max = 65535)
    @Schema(description = "输入 JSON Schema")
    private String inputSchemaJson;
    @NotBlank
    @Size(max = 65535)
    @Schema(description = "输出 JSON Schema")
    private String outputSchemaJson;
    @NotNull
    @Schema(description = "是否启用")
    private Boolean enabled;
}
