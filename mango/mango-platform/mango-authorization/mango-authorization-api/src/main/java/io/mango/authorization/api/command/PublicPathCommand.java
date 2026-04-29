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
 * 公共路径创建或修改命令。
 *
 * @author Mango
 */
@Data
@Schema(description = "公共路径添加/修改请求")
public class PublicPathCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID (更新时必填)")
    private Long id;

    @NotBlank(message = "路径不能为空")
    @Size(max = 255, message = "路径最多255个字符")
    @Schema(description = "路径模式 (支持通配符如 /public/**)")
    private String path;

    @NotNull(message = "路径类型不能为空")
    @Min(value = 1, message = "路径类型最小值为1")
    @Max(value = 4, message = "路径类型最大值为4")
    @Schema(description = "路径类型: 1-匿名, 2-登录, 3-权限, 4-内部")
    private Integer pathType;

    @Size(max = 200, message = "描述最多200个字符")
    @Schema(description = "描述")
    private String description;

    @Min(value = 0, message = "优先级最小值为0")
    @Schema(description = "优先级")
    private Integer priority;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
