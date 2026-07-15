package io.mango.authorization.api;

import io.mango.common.result.R;
import io.mango.authorization.api.command.MenuCommand;
import io.mango.authorization.api.query.MenuTreeQuery;
import io.mango.authorization.api.vo.MenuVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 菜单管理 API 契约。
 */
public interface MenuApi {

    /**
     * 查询菜单资源。
     *
     * @param query 菜单查询条件
     * @return 菜单列表或树
     */
    R<List<MenuVO>> getMenus(@Valid MenuTreeQuery query);

    /**
     * 查询当前用户菜单。
     *
     * @param query 菜单查询条件
     * @return 菜单列表或树
     */
    R<List<MenuVO>> getUserMenus(@Valid MenuTreeQuery query);

    /** 获取菜单详情。 */
    R<MenuVO> getById(@Positive Long menuId);

    /** 新增菜单。 */
    R<Void> add(@Valid MenuCommand command);

    /** 更新菜单。 */
    R<Void> update(@Valid MenuCommand command);

    /** 删除菜单。 */
    R<Void> delete(@Positive Long menuId);
}
