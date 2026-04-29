package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.command.AssignRoleMenusCommand;
import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.vo.RoleVO;

import java.util.List;

/**
 * 角色管理 API 契约。
 */
public interface RoleApi {

    /**
     * 查询角色列表。
     *
     * @return 角色列表
     */
    R<List<RoleVO>> list();

    /**
     * 查询角色详情。
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    R<RoleVO> get(Long id);

    /**
     * 创建角色。
     *
     * @param command 角色数据
     * @return 新角色 ID
     */
    R<Long> create(RoleCommand command);

    /**
     * 更新角色。
     *
     * @param command 角色数据
     * @return 是否成功
     */
    R<Boolean> update(RoleCommand command);

    /**
     * 删除角色。
     *
     * @param id 角色 ID
     * @return 是否成功
     */
    R<Boolean> delete(Long id);

    /**
     * 查询主体角色。
     *
     * @param subjectId 主体 ID
     * @return 主体已绑定角色
     */
    R<List<RoleVO>> getSubjectRoles(Long subjectId);

    /**
     * 给主体分配角色。
     *
     * @param subjectId 主体 ID
     * @param command 分配命令
     * @return 是否成功
     */
    R<Boolean> assignRoles(Long subjectId, AssignSubjectRolesCommand command);

    /**
     * 查询角色菜单 ID。
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    R<List<Long>> getRoleMenuIds(Long roleId);

    /**
     * 给角色分配菜单。
     *
     * @param roleId 角色 ID
     * @param command 分配命令
     * @return 是否成功
     */
    R<Boolean> assignMenus(Long roleId, AssignRoleMenusCommand command);
}
