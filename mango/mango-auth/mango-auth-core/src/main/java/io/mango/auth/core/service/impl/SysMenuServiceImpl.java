package io.mango.auth.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.auth.api.po.SysMenuPo;
import io.mango.auth.core.service.ISysMenuService;
import io.mango.common.exception.BizException;
import io.mango.permission.core.entity.SysMenu;
import io.mango.permission.core.entity.SysRoleMenu;
import io.mango.permission.api.vo.SysMenuVO;
import io.mango.permission.core.mapper.SysMenuMapper;
import io.mango.permission.core.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("authSysMenuServiceImpl")
@RequiredArgsConstructor
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    @Transactional
    public Long addMenu(SysMenuPo po) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(System.currentTimeMillis());
        menu.setMenuName(po.getMenuName());
        menu.setMenuCode(po.getMenuCode());
        menu.setMenuType(po.getMenuType());
        menu.setParentId(po.getParentId() != null ? po.getParentId() : 0L);
        menu.setPath(po.getPath());
        menu.setComponent(po.getComponent());
        menu.setIcon(po.getIcon());
        menu.setSort(po.getSort() != null ? po.getSort() : 0);
        menu.setStatus(po.getStatus() != null ? po.getStatus() : 1);
        menu.setVisible(po.getVisible() != null ? po.getVisible() : 1);
        menu.setKeepAlive(po.getKeepAlive() != null ? po.getKeepAlive() : 0);
        menu.setEmbedded(po.getEmbedded() != null ? po.getEmbedded() : 0);
        menu.setPermissions(po.getPermission());
        menu.setGroupId(po.getGroupId());
        menuMapper.insert(menu);
        return menu.getMenuId();
    }

    @Override
    @Transactional
    public Boolean updateMenu(SysMenuPo po) {
        if (po.getMenuId() == null) {
            throw new BizException("菜单ID不能为空");
        }
        SysMenu menu = menuMapper.selectById(po.getMenuId());
        if (menu == null) {
            throw new BizException("菜单不存在");
        }
        if (po.getMenuName() != null) menu.setMenuName(po.getMenuName());
        if (po.getMenuCode() != null) menu.setMenuCode(po.getMenuCode());
        if (po.getMenuType() != null) menu.setMenuType(po.getMenuType());
        if (po.getParentId() != null) menu.setParentId(po.getParentId());
        if (po.getPath() != null) menu.setPath(po.getPath());
        if (po.getComponent() != null) menu.setComponent(po.getComponent());
        if (po.getIcon() != null) menu.setIcon(po.getIcon());
        if (po.getSort() != null) menu.setSort(po.getSort());
        if (po.getStatus() != null) menu.setStatus(po.getStatus());
        if (po.getVisible() != null) menu.setVisible(po.getVisible());
        if (po.getPermission() != null) menu.setPermissions(po.getPermission());
        menuMapper.updateById(menu);
        return true;
    }

    @Override
    @Transactional
    public Boolean deleteMenu(Long menuId) {
        // 逻辑删除
        LambdaUpdateWrapper<SysMenu> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysMenu::getMenuId, menuId)
               .set(SysMenu::getStatus, 0);
        menuMapper.update(null, wrapper);
        return true;
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean assignMenus(Long roleId, List<Long> menuIds) {
        LambdaQueryWrapper<SysRoleMenu> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SysRoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(delWrapper);
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
        return true;
    }

    @Override
    public List<SysMenuVO> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        return menus.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private SysMenuVO convertToVO(SysMenu menu) {
        SysMenuVO vo = new SysMenuVO();
        vo.setMenuId(menu.getMenuId());
        vo.setParentId(menu.getParentId());
        vo.setMenuType(menu.getMenuType());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuCode(menu.getMenuCode());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setStatus(menu.getStatus());
        vo.setVisible(menu.getVisible());
        vo.setKeepAlive(menu.getKeepAlive());
        vo.setEmbedded(menu.getEmbedded());
        vo.setRedirect(menu.getRedirect());
        vo.setPermissions(menu.getPermissions());
        SysMenuVO.MenuMeta meta = new SysMenuVO.MenuMeta();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        vo.setMeta(meta);
        List<SysMenu> children = listByParentId(menu.getMenuId());
        if (!children.isEmpty()) {
            vo.setChildren(buildMenuTree(children));
        }
        return vo;
    }

    private List<SysMenu> listByParentId(Long parentId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, parentId)
               .orderByAsc(SysMenu::getSort);
        return menuMapper.selectList(wrapper);
    }
}
