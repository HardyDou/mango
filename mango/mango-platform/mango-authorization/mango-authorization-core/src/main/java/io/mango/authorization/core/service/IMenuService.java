package io.mango.authorization.core.service;

import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.api.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口。
 */
public interface IMenuService {

    /**
     * 查询当前用户菜单树。
     */
    List<MenuVO> getUserMenus(String appCode, Integer type, Long parentId, Long userId);

    /**
     * 查询管理端菜单树。
     */
    List<MenuVO> getTreeMenus(String appCode, Long parentId, String menuName);

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
