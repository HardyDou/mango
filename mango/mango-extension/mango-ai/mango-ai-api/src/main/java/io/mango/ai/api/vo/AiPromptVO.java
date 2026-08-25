package io.mango.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.mango.ai.api.enums.AiPromptStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Prompt 模板返回对象。 */
@Getter
@Setter
public class AiPromptVO {
    @Schema(description = "记录标识")
    private Long id;
    @Schema(description = "配置编码")
    private String code;
    @Schema(description = "显示名称")
    private String name;
    @Schema(description = "业务说明")
    private String description;
    @Schema(description = "Prompt 模板正文")
    private String template;
    @Schema(description = "Prompt 变量定义 JSON")
    private String variablesJson;
    @Schema(description = "Prompt 生命周期状态")
    private AiPromptStatus status;
    @Schema(description = "Prompt 版本号")
    private Integer version;
    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
