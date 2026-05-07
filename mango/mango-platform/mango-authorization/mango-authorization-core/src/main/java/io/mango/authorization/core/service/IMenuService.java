package io.mango.authorization.core.service;

import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.api.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口。
 */
public interface IMenuService {

    /**
     * 查询菜单资源列表或树。
     */
    List<MenuVO> listMenus(String appCode, Integer type, Long parentId, String menuName, Integer status, boolean tree);

    /**
     * 查询当前用户菜单列表或树。
     */
    List<MenuVO> listUserMenus(String appCode, Integer type, Long parentId, Long userId, boolean tree);

    /**
     * 按 ID 查询菜单。
     */
    Menu getById(Long menuId);

    /**
     * 按父菜单 ID 查询子菜单。
     */
    List<Menu> listByParentId(Long parentId);

    /**
     * 将菜单列表组装为树。
     */
    List<MenuVO> buildMenuTree(List<Menu> menus);

    /**
     * 新增菜单。
     */
    boolean addMenu(Menu menu);

    /**
     * 更新菜单。
     */
    boolean updateMenu(Long menuId, Menu menu);

    /**
     * 删除菜单。
     */
    boolean deleteMenu(Long menuId);
}
