package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.command.MenuCommand;
import io.mango.authorization.api.query.MenuTreeQuery;
import io.mango.authorization.api.vo.MenuVO;

import java.util.List;
import java.util.Set;

/**
 * 菜单管理 API 契约。
 */
public interface MenuApi {

    /**
     * 获取当前用户菜单树。
     *
     * @param query 菜单查询条件
     * @return 菜单树
     */
    R<List<MenuVO>> getUserMenus(MenuTreeQuery query);

    /**
     * 获取全部菜单树。
     *
     * @param query 菜单查询条件
     * @return 菜单树
     */
    R<List<MenuVO>> getTreeMenus(MenuTreeQuery query);

    /** 获取菜单详情。 */
    R<MenuVO> getById(Long menuId);

    /**
     * 获取用户权限码。
     *
     * @param userId 用户 ID
     * @return 权限码集合
     */
    R<Set<String>> getUserPermissions(Long userId);

    /** 新增菜单。 */
    R<Void> add(MenuCommand command);

    /** 更新菜单。 */
    R<Void> update(MenuCommand command);

    /** 删除菜单。 */
    R<Void> delete(Long menuId);
}
