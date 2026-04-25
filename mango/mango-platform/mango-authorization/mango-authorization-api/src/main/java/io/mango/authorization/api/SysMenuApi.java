package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.vo.SysMenuVO;

import java.util.List;
import java.util.Set;

/**
 * System menu remote API
 * Exposed via Controller (local) or Feign Client (remote)
 *
 * @author Mango
 */
public interface SysMenuApi {

    /**
     * Get current user's menu list (tree structure)
     *
     * @param type     menu type: null=all, 0=directory, 1=menu, 2=button
     * @param parentId parent menu ID, 0=root
     * @return menu tree
     */
    R<List<SysMenuVO>> getUserMenus(Integer type, Long parentId);

    /**
     * Get all menus (tree structure, for admin)
     *
     * @param parentId  parent menu ID
     * @param menuName  menu name (fuzzy search)
     * @return menu tree
     */
    R<List<SysMenuVO>> getTreeMenus(Long parentId, String menuName);

    /**
     * Get user's permission codes
     *
     * @param userId user ID
     * @return set of permission codes (e.g., system:user:view)
     */
    Set<String> getUserPermissions(Long userId);
}
