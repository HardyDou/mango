package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiCapability;
import io.mango.ai.api.enums.AiServiceType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** AI 服务定义返回对象。 */
@Getter
@Setter
public class AiServiceVO {
    @Schema(description = "记录标识")
    private Long id;
    @Schema(description = "配置编码")
    private String code;
    @Schema(description = "显示名称")
    private String name;
    @Schema(description = "业务说明")
    private String description;
    @Schema(description = "AI 服务类型")
    private AiServiceType serviceType;
    @Schema(description = "AI 能力类型")
    private AiCapability capability;
    @Schema(description = "关联 Prompt 标识")
    private Long promptId;
    @Schema(description = "关联 Prompt 名称")
    private String promptName;
    @Schema(description = "关联 Skill 标识")
    private Long skillId;
    @Schema(description = "关联 Skill 名称")
    private String skillName;
    @Schema(description = "输入 JSON Schema")
    private String inputSchemaJson;
    @Schema(description = "输出 JSON Schema")
    private String outputSchemaJson;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
