package io.mango.rbac.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.rbac.core.entity.SysMenu;
import io.mango.rbac.api.vo.SysMenuVO;
import io.mango.rbac.core.mapper.SysMenuMapper;
import io.mango.rbac.core.service.ISysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * System menu service implementation
 *
 * @author Mango
 */
@Slf4j
@Service("permissionSysMenuServiceImpl")
@RequiredArgsConstructor
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenuVO> getUserMenus(Integer type, Long parentId, Long userId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(parentId != null && parentId > 0, SysMenu::getParentId, parentId)
                .eq(parentId == null || parentId == 0, SysMenu::getParentId, 0)
                .eq(type != null, SysMenu::getMenuType, type)
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getVisible, 1)
                .orderByAsc(SysMenu::getSort);

        List<SysMenu> menus = sysMenuMapper.selectList(wrapper);
        return buildMenuTree(menus);
    }

    @Override
    public List<SysMenuVO> getTreeMenus(Long parentId, String menuName) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(parentId != null && parentId > 0, SysMenu::getParentId, parentId)
                .like(menuName != null && !menuName.isEmpty(), SysMenu::getMenuName, menuName)
                .orderByAsc(SysMenu::getSort);

        List<SysMenu> menus = sysMenuMapper.selectList(wrapper);
        return buildMenuTree(menus);
    }

    @Override
    public SysMenu getById(Long menuId) {
        return sysMenuMapper.selectById(menuId);
    }

    @Override
    public List<SysMenu> listByParentId(Long parentId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, parentId)
                .orderByAsc(SysMenu::getSort);
        return sysMenuMapper.selectList(wrapper);
    }

    /**
     * Build menu tree using in-memory filtering (avoids N+1 queries).
     * Uses full menu list and filters by parentId recursively.
     */
    @Override
    public List<SysMenuVO> buildMenuTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        // Start from root nodes (parentId = 0)
        return buildMenuTreeRecursive(menus, 0L);
    }

    private List<SysMenuVO> buildMenuTreeRecursive(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    SysMenuVO vo = convertToMenuVO(m);
                    List<SysMenuVO> children = buildMenuTreeRecursive(allMenus, m.getMenuId());
                    if (!children.isEmpty()) {
                        vo.setChildren(children);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private SysMenuVO convertToMenuVO(SysMenu menu) {
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

        // Build meta
        SysMenuVO.MenuMeta meta = new SysMenuVO.MenuMeta();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        vo.setMeta(meta);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMenu(SysMenu menu) {
        if (menu == null) {
            log.warn("Add menu failed: menu is null");
            return false;
        }
        // Validate required fields
        if (menu.getMenuName() == null || menu.getMenuName().isBlank()) {
            log.warn("Add menu failed: menuName is required");
            return false;
        }
        if (menu.getMenuCode() == null || menu.getMenuCode().isBlank()) {
            log.warn("Add menu failed: menuCode is required");
            return false;
        }
        try {
            int rows = sysMenuMapper.insert(menu);
            return rows > 0;
        } catch (Exception e) {
            log.error("Add menu failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(Long menuId, SysMenu menu) {
        if (menuId == null || menu == null) {
            log.warn("Update menu failed: invalid parameters");
            return false;
        }
        try {
            SysMenu existing = sysMenuMapper.selectById(menuId);
            if (existing == null) {
                log.warn("Update menu failed: menu not found, menuId={}", menuId);
                return false;
            }
            // Use partial update - only update non-null fields
            LambdaUpdateWrapper<SysMenu> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SysMenu::getMenuId, menuId);
            if (menu.getGroupId() != null) wrapper.set(SysMenu::getGroupId, menu.getGroupId());
            if (menu.getParentId() != null) wrapper.set(SysMenu::getParentId, menu.getParentId());
            if (menu.getMenuType() != null) wrapper.set(SysMenu::getMenuType, menu.getMenuType());
            if (menu.getMenuName() != null) wrapper.set(SysMenu::getMenuName, menu.getMenuName());
            if (menu.getMenuCode() != null) wrapper.set(SysMenu::getMenuCode, menu.getMenuCode());
            if (menu.getPath() != null) wrapper.set(SysMenu::getPath, menu.getPath());
            if (menu.getIcon() != null) wrapper.set(SysMenu::getIcon, menu.getIcon());
            if (menu.getSort() != null) wrapper.set(SysMenu::getSort, menu.getSort());
            if (menu.getStatus() != null) wrapper.set(SysMenu::getStatus, menu.getStatus());
            if (menu.getVisible() != null) wrapper.set(SysMenu::getVisible, menu.getVisible());
            if (menu.getComponent() != null) wrapper.set(SysMenu::getComponent, menu.getComponent());
            if (menu.getKeepAlive() != null) wrapper.set(SysMenu::getKeepAlive, menu.getKeepAlive());
            if (menu.getEmbedded() != null) wrapper.set(SysMenu::getEmbedded, menu.getEmbedded());
            if (menu.getRedirect() != null) wrapper.set(SysMenu::getRedirect, menu.getRedirect());
            if (menu.getPermissions() != null) wrapper.set(SysMenu::getPermissions, menu.getPermissions());
            if (menu.getRemark() != null) wrapper.set(SysMenu::getRemark, menu.getRemark());
            // Note: createBy, createTime, delFlag should not be updated

            int rows = sysMenuMapper.update(null, wrapper);
            return rows > 0;
        } catch (Exception e) {
            log.error("Update menu failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long menuId) {
        if (menuId == null) {
            log.warn("Delete menu failed: menuId is null");
            return false;
        }
        try {
            // Check if menu has children
            List<SysMenu> children = listByParentId(menuId);
            if (!children.isEmpty()) {
                log.warn("Delete menu failed: menu has children, menuId={}", menuId);
                return false;
            }
            int rows = sysMenuMapper.deleteById(menuId);
            return rows > 0;
        } catch (Exception e) {
            log.error("Delete menu failed: {}", e.getMessage());
            return false;
        }
    }
}
