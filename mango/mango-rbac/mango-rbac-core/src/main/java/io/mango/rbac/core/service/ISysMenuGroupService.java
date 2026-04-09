package io.mango.rbac.core.service;

import io.mango.rbac.core.entity.SysMenuGroup;
import io.mango.rbac.api.vo.SysMenuGroupVO;

import java.util.List;

/**
 * Menu Group Service interface
 *
 * @author Mango
 */
public interface ISysMenuGroupService {

    /**
     * Get all menu groups with menus tree
     *
     * @return menu groups
     */
    List<SysMenuGroupVO> listMenuGroups();

    /**
     * Get single menu group by ID
     *
     * @param groupId group ID
     * @return menu group
     */
    SysMenuGroupVO getMenuGroup(Long groupId);

    /**
     * Create menu group
     *
     * @param menuGroup menu group data
     * @return created group ID
     */
    Long createMenuGroup(SysMenuGroupVO menuGroup);

    /**
     * Update menu group
     *
     * @param menuGroup menu group data
     */
    void updateMenuGroup(SysMenuGroupVO menuGroup);

    /**
     * Delete menu group
     *
     * @param groupId group ID
     */
    void deleteMenuGroup(Long groupId);
}
