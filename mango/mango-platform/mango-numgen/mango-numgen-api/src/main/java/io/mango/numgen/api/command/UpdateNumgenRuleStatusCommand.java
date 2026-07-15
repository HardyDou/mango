package io.mango.numgen.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新编号规则状态命令。
 */
@Data
@Schema(description = "更新编号规则状态命令")
public class UpdateNumgenRuleStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "编号规则 ID 不能为空")
    @Positive(message = "编号规则 ID 必须大于0")
    @Schema(description = "编号规则 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
