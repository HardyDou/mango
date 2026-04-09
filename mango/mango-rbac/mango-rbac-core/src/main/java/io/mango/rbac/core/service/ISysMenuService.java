package io.mango.rbac.core.service;

import io.mango.rbac.core.entity.SysMenu;
import io.mango.rbac.api.vo.SysMenuVO;

import java.util.List;

/**
 * System menu service interface
 *
 * @author Mango
 */
public interface ISysMenuService {

    /**
     * Get current user's menu list (tree structure)
     *
     * @param type     menu type: null=all, 0=directory, 1=menu, 2=button
     * @param parentId parent menu ID, 0=root
     * @param userId   current user ID
     * @return menu tree
     */
    List<SysMenuVO> getUserMenus(Integer type, Long parentId, Long userId);

    /**
     * Get all menus (tree structure, for admin)
     *
     * @param parentId  parent menu ID
     * @param menuName  menu name (fuzzy search)
     * @return menu tree
     */
    List<SysMenuVO> getTreeMenus(Long parentId, String menuName);

    /**
     * Get menu by ID
     *
     * @param menuId menu ID
     * @return menu entity
     */
    SysMenu getById(Long menuId);

    /**
     * List menus by parent ID
     *
     * @param parentId parent menu ID
     * @return menu list
     */
    List<SysMenu> listByParentId(Long parentId);

    /**
     * Build menu tree from flat list
     *
     * @param menus flat menu list
     * @return tree structure
     */
    List<SysMenuVO> buildMenuTree(List<SysMenu> menus);

    /**
     * Add a new menu
     *
     * @param menu menu to add
     * @return true if successful
     */
    boolean addMenu(SysMenu menu);

    /**
     * Update an existing menu
     *
     * @param menuId menu ID to update
     * @param menu menu data
     * @return true if successful
     */
    boolean updateMenu(Long menuId, SysMenu menu);

    /**
     * Delete a menu
     *
     * @param menuId menu ID to delete
     * @return true if successful
     */
    boolean deleteMenu(Long menuId);
}
