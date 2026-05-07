package io.mango.authorization.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 给角色分配菜单命令。
 *
 * @author Mango
 */
@Data
@Schema(description = "给角色分配菜单命令")
public class AssignRoleMenusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}
