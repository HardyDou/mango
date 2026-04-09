package io.mango.rbac.api.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Public path PO for add/update requests
 * Contains validation annotations for request validation
 *
 * @author Mango
 */
@Data
@Schema(description = "公共路径添加/修改请求")
public class SysPublicPathPo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID (null for add, required for update)
     */
    @Schema(description = "ID (更新时必填)")
    private Long id;

    /**
     * Path pattern (supports wildcards like /public/**)
     */
    @NotBlank(message = "路径不能为空")
    @Size(max = 255, message = "路径最多255个字符")
    @Schema(description = "路径模式 (支持通配符如 /public/**)")
    private String path;

    /**
     * Path type: 1=anonymous, 2=login, 3=permission
     */
    @NotNull(message = "路径类型不能为空")
    @Min(value = 1, message = "路径类型最小值为1")
    @Max(value = 3, message = "路径类型最大值为3")
    @Schema(description = "路径类型: 1-匿名访问, 2-登录访问, 3-权限访问")
    private Integer pathType;

    /**
     * Description
     */
    @Size(max = 200, message = "描述最多200个字符")
    @Schema(description = "描述")
    private String description;

    /**
     * Priority (higher number = higher priority)
     */
    @Min(value = 0, message = "优先级最小值为0")
    @Schema(description = "优先级 (数值越大优先级越高)")
    private Integer priority;

    /**
     * Status: 0=disabled, 1=enabled
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 1, message = "状态最大值为1")
    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
