package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiToolType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** AI 工具返回对象。 */
@Getter
@Setter
public class AiToolVO {
    @Schema(description = "记录标识")
    private Long id;
    @Schema(description = "配置编码")
    private String code;
    @Schema(description = "显示名称")
    private String name;
    @Schema(description = "业务说明")
    private String description;
    @Schema(description = "工具接入类型")
    private AiToolType toolType;
    @Schema(description = "工具调用端点")
    private String endpoint;
    @Schema(description = "输入 JSON Schema")
    private String inputSchemaJson;
    @Schema(description = "输出 JSON Schema")
    private String outputSchemaJson;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
