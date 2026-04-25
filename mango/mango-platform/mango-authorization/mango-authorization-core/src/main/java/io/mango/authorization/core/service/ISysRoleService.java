package io.mango.authorization.core.service;

import io.mango.authorization.api.po.SysRolePo;
import io.mango.authorization.api.vo.SysRoleVO;

import java.util.List;

/**
 * System role internal service interface.
 * Defined in authorization-core, not exposed across modules.
 *
 * <p>Role management capabilities include CRUD operations on roles,
 * assignment of roles to subjects, and assignment of menus to roles.
 * All operations are tenant-aware via TenantContextHolder.
 *
 * @author Mango
 */
public interface ISysRoleService {

    /**
     * List all enabled roles ordered by sort order.
     *
     * @return list of all enabled roles
     */
    List<SysRoleVO> list();

    /**
     * Get a role by its ID.
     *
     * @param id the role ID
     * @return the role VO, or null if not found
     */
    SysRoleVO get(Long id);

    /**
     * Create a new role.
     * The tenantId is automatically set from TenantContextHolder.
     *
     * @param po the role creation data
     * @return the created role ID
     */
    Long create(SysRolePo po);

    /**
     * Update an existing role.
     *
     * @param po the role update data (must include roleId)
     * @return true if role was updated, false if roleId is null or role not found
     */
    Boolean update(SysRolePo po);

    /**
     * Delete a role and all its subject-role and role-menu relationships.
     *
     * @param id the role ID to delete
     * @return true if role was deleted, false if role not found
     */
    Boolean delete(Long id);

    /**
     * Get all roles assigned to a specific subject.
     *
     * @param subjectId the subject ID
     * @return list of roles assigned to the subject, empty list if none
     */
    List<SysRoleVO> getSubjectRoles(Long subjectId);

    /**
     * Assign roles to a subject.
     * This replaces all existing role assignments for the subject.
     * The tenantId is automatically set from TenantContextHolder.
     *
     * @param subjectId the subject ID to assign roles to
     * @param roleIds list of role IDs to assign (replaces existing)
     * @return true on success
     */
    Boolean assignRoles(Long subjectId, List<Long> roleIds);

    /**
     * Get all menu IDs assigned to a specific role.
     *
     * @param roleId the role ID
     * @return list of menu IDs assigned to the role, empty list if none
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * Assign menus to a role.
     * This replaces all existing menu assignments for the role.
     * The tenantId is automatically set from TenantContextHolder.
     *
     * @param roleId the role ID to assign menus to
     * @param menuIds list of menu IDs to assign (replaces existing)
     * @return true on success
     */
    Boolean assignMenus(Long roleId, List<Long> menuIds);
}
