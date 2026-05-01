package io.mango.authorization.api.command;

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
public class AssignRoleMenusCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    private List<Long> menuIds;
}
