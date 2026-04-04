package io.mango.auth.core.service;

import io.mango.auth.api.po.SysMenuPo;
import io.mango.permission.api.vo.SysMenuVO;

import java.util.List;

public interface ISysMenuService {
    Long addMenu(SysMenuPo po);
    Boolean updateMenu(SysMenuPo po);
    Boolean deleteMenu(Long menuId);
    List<Long> getRoleMenuIds(Long roleId);
    Boolean assignMenus(Long roleId, List<Long> menuIds);
    List<SysMenuVO> buildMenuTree(List<io.mango.permission.core.entity.SysMenu> menus);
}
