package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.po.SysRolePo;
import io.mango.authorization.api.vo.SysRoleVO;

import java.util.List;

/**
 * System role API (exposed type).
 * Provides role management capabilities for external consumers.
 *
 * @author Mango
 */
public interface SysRoleApi {

    /**
     * List all roles
     *
     * @return role list
     */
    R<List<SysRoleVO>> list();

    /**
     * Get role by ID
     *
     * @param id role ID
     * @return role details
     */
    R<SysRoleVO> get(Long id);

    /**
     * Create a new role
     *
     * @param po role data
     * @return created role ID
     */
    R<Long> create(SysRolePo po);

    /**
     * Update an existing role
     *
     * @param po role data
     * @return success flag
     */
    R<Boolean> update(SysRolePo po);

    /**
     * Delete a role
     *
     * @param id role ID
     * @return success flag
     */
    R<Boolean> delete(Long id);

    /**
     * Get user's roles
     *
     * @param userId user ID
     * @return role list for the user
     */
    R<List<SysRoleVO>> getUserRoles(Long userId);

    /**
     * Assign roles to a user
     *
     * @param userId  user ID
     * @param roleIds role IDs to assign
     * @return success flag
     */
    R<Boolean> assignRoles(Long userId, List<Long> roleIds);

    /**
     * Get menu IDs assigned to a role
     *
     * @param roleId role ID
     * @return menu IDs
     */
    R<List<Long>> getRoleMenuIds(Long roleId);

    /**
     * Assign menus to a role
     *
     * @param roleId  role ID
     * @param menuIds menu IDs to assign
     * @return success flag
     */
    R<Boolean> assignMenus(Long roleId, List<Long> menuIds);
}
