package io.mango.permission.api;

import io.mango.common.result.R;
import io.mango.permission.api.vo.SysMenuGroupVO;

import java.util.List;

/**
 * Menu Group API interface
 *
 * @author Mango
 */
public interface MenuGroupApi {

    /**
     * Get all menu groups (with menus tree)
     *
     * @return menu groups with menus
     */
    R<List<SysMenuGroupVO>> listMenuGroups();

    /**
     * Get single menu group by ID
     *
     * @param groupId group ID
     * @return menu group
     */
    R<SysMenuGroupVO> getMenuGroup(Long groupId);

    /**
     * Create menu group
     *
     * @param menuGroup menu group data
     * @return created group ID
     */
    R<Long> createMenuGroup(SysMenuGroupVO menuGroup);

    /**
     * Update menu group
     *
     * @param menuGroup menu group data
     */
    R<Void> updateMenuGroup(SysMenuGroupVO menuGroup);

    /**
     * Delete menu group
     *
     * @param groupId group ID
     */
    R<Void> deleteMenuGroup(Long groupId);
}
