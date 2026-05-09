package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用登录上下文创建或修改命令。
 */
@Data
@Schema(description = "应用登录上下文创建或修改命令")
public class AppLoginContextCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "上下文ID，创建时为空，修改时可为空")
    private Long contextId;

    @Schema(description = "登录域")
    @NotBlank(message = "登录域不能为空")
    @Size(max = 32, message = "登录域最多32个字符")
    private String realm;

    @Schema(description = "操作者类型")
    @NotBlank(message = "操作者类型不能为空")
    @Size(max = 32, message = "操作者类型最多32个字符")
    private String actorType;

    @Schema(description = "是否默认上下文：0-否，1-是")
    @NotNull(message = "是否默认上下文不能为空")
    @Min(value = 0, message = "是否默认上下文最小值为0")
    @Max(value = 1, message = "是否默认上下文最大值为1")
    private Integer defaultFlag;

    @Schema(description = "状态：0-禁用，1-启用")
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    private Integer status;

    @Schema(description = "排序号")
    @Min(value = 0, message = "排序号最小值为0")
    private Integer sort;
}
