package io.mango.authorization.core.service;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.MenuCommand;
import io.mango.authorization.api.query.MenuTreeQuery;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.api.vo.MenuVO;

import java.util.List;
import java.util.Set;

/**
 * 菜单服务接口。
 */
public interface IMenuService {

    /**
     * 查询菜单资源列表或树。
     */
    List<MenuVO> listMenus(MenuTreeQuery query);

    /**
     * 查询当前用户菜单列表或树。
     */
    List<MenuVO> listUserMenus(MenuTreeQuery query, AuthorizationQuery authorizationQuery);

    MenuVO getMenu(Long menuId);

    Void createMenu(MenuCommand command);

    Void updateMenu(MenuCommand command);

    Void removeMenu(Long menuId);

    /**
     * 按 ID 查询菜单。
     */
    MenuEntity getById(Long menuId);

    /**
     * 按父菜单 ID 查询子菜单。
     */
    List<MenuEntity> listByParentId(Long parentId);

    /**
     * 将菜单列表组装为树。
     */
    List<MenuVO> buildMenuTree(List<MenuEntity> menus);

    /**
     * 查询所有启用菜单/按钮声明的权限码。
     */
    Set<String> listAllPermissionCodes();

    /**
     * 新增菜单。
     */
    boolean addMenu(MenuEntity menu);

    /**
     * 更新菜单。
     */
    boolean updateMenu(Long menuId, MenuEntity menu);

    /**
     * 删除菜单。
     */
    boolean deleteMenu(Long menuId);
}
