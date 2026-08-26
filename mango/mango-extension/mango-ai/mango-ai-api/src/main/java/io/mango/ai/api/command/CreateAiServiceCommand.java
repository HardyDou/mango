package io.mango.ai.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 创建 AI 服务定义。 */
@Getter
@Setter
public class CreateAiServiceCommand {
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
    @Schema(description = "AI 服务类型")
    private AiServiceType serviceType;
    @Schema(description = "AI 能力类型")
    private AiCapability capability;
    @Schema(description = "关联 Prompt 标识")
    @Positive(message = "Prompt 标识必须大于0")
    private Long promptId;
    @Schema(description = "关联 Skill 标识")
    @Positive(message = "Skill 标识必须大于0")
    private Long skillId;
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
