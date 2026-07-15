package io.mango.numgen.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存编号规则命令。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "保存编号规则命令")
public class SaveNumgenRuleCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Positive(message = "编号规则 ID 必须大于0")
    @Schema(description = "编号规则 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "规则键不能为空")
    @Size(max = 128, message = "规则键不能超过128个字符")
    @Schema(description = "编号规则键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String genKey;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称不能超过128个字符")
    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;

    @Positive(message = "规则版本必须大于0")
    @Schema(description = "规则版本，默认 1")
    private Integer version;

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Min(value = 0, message = "发布状态只能为0或1")
    @Max(value = 1, message = "发布状态只能为0或1")
    @Schema(description = "发布状态：0-未生效，1-生效中")
    private Integer publishStatus;
}
