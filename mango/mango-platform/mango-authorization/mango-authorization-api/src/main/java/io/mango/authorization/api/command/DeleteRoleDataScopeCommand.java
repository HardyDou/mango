package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 删除角色数据权限命令。
 */
@Data
@Schema(description = "删除角色数据权限命令")
public class DeleteRoleDataScopeCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色 ID")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "资源编码")
    @NotBlank(message = "资源编码不能为空")
    @Size(max = 128, message = "资源编码最多128个字符")
    private String resourceCode;
}
