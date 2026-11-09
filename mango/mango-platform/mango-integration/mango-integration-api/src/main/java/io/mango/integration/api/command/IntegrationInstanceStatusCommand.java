package io.mango.integration.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 连接实例状态变更命令。
 */
@Data
@Schema(description = "连接实例状态变更命令")
public class IntegrationInstanceStatusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "实例编码不能为空")
    @Size(max = 64, message = "实例编码最多64个字符")
    @Schema(description = "实例编码")
    private String instanceCode;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @Schema(description = "状态：0-停用，1-启用")
    private Integer status;
}
