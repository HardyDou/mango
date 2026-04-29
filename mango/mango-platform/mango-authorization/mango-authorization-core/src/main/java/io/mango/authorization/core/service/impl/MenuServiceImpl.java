package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.service.IMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现。
 */
@Slf4j
@Service("permissionMenuServiceImpl")
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

    private final MenuMapper menuMapper;

    @Override
    public List<MenuVO> getUserMenus(String appCode, Integer type, Long parentId, Long userId) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(appCode != null && !appCode.isBlank(), Menu::getAppCode, appCode)
                .eq(parentId != null && parentId > 0, Menu::getParentId, parentId)
                .eq(parentId == null || parentId == 0, Menu::getParentId, 0)
                .eq(type != null, Menu::getMenuType, type)
                .eq(Menu::getStatus, 1)
                .eq(Menu::getVisible, 1)
                .orderByAsc(Menu::getSort);

        List<Menu> menus = menuMapper.selectList(wrapper);
        return buildMenuTree(menus);
    }

    @Override
    public List<MenuVO> getTreeMenus(String appCode, Long parentId, String menuName) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(appCode != null && !appCode.isBlank(), Menu::getAppCode, appCode)
                .eq(parentId != null && parentId > 0, Menu::getParentId, parentId)
                .like(menuName != null && !menuName.isEmpty(), Menu::getMenuName, menuName)
                .orderByAsc(Menu::getSort);

        List<Menu> menus = menuMapper.selectList(wrapper);
        return buildMenuTree(menus);
    }

    @Override
    public Menu getById(Long menuId) {
        return menuMapper.selectById(menuId);
    }

    @Override
    public List<Menu> listByParentId(Long parentId) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getParentId, parentId)
                .orderByAsc(Menu::getSort);
        return menuMapper.selectList(wrapper);
    }

    /**
     * 基于内存递归组装菜单树，避免 N+1 查询。
     */
    @Override
    public List<MenuVO> buildMenuTree(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        // 从根节点开始组装。
        return buildMenuTreeRecursive(menus, 0L);
    }

    private List<MenuVO> buildMenuTreeRecursive(List<Menu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    MenuVO vo = convertToMenuVO(m);
                    List<MenuVO> children = buildMenuTreeRecursive(allMenus, m.getMenuId());
                    if (!children.isEmpty()) {
                        vo.setChildren(children);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private MenuVO convertToMenuVO(Menu menu) {
        MenuVO vo = new MenuVO();
        vo.setMenuId(menu.getMenuId());
        vo.setAppCode(menu.getAppCode());
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

        // 构造前端路由元信息。
        MenuVO.MenuMeta meta = new MenuVO.MenuMeta();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        vo.setMeta(meta);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMenu(Menu menu) {
        if (menu == null) {
            log.warn("Add menu failed: menu is null");
            return false;
        }
        // 校验必填字段。
        if (menu.getMenuName() == null || menu.getMenuName().isBlank()) {
            log.warn("Add menu failed: menuName is required");
            return false;
        }
        if (menu.getMenuCode() == null || menu.getMenuCode().isBlank()) {
            log.warn("Add menu failed: menuCode is required");
            return false;
        }
        try {
            int rows = menuMapper.insert(menu);
            return rows > 0;
        } catch (Exception e) {
            log.error("Add menu failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(Long menuId, Menu menu) {
        if (menuId == null || menu == null) {
            log.warn("Update menu failed: invalid parameters");
            return false;
        }
        try {
            Menu existing = menuMapper.selectById(menuId);
            if (existing == null) {
                log.warn("Update menu failed: menu not found, menuId={}", menuId);
                return false;
            }
            // 只更新非空字段。
            LambdaUpdateWrapper<Menu> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Menu::getMenuId, menuId);
            if (menu.getAppCode() != null) wrapper.set(Menu::getAppCode, menu.getAppCode());
            if (menu.getParentId() != null) wrapper.set(Menu::getParentId, menu.getParentId());
            if (menu.getMenuType() != null) wrapper.set(Menu::getMenuType, menu.getMenuType());
            if (menu.getMenuName() != null) wrapper.set(Menu::getMenuName, menu.getMenuName());
            if (menu.getMenuCode() != null) wrapper.set(Menu::getMenuCode, menu.getMenuCode());
            if (menu.getPath() != null) wrapper.set(Menu::getPath, menu.getPath());
            if (menu.getIcon() != null) wrapper.set(Menu::getIcon, menu.getIcon());
            if (menu.getSort() != null) wrapper.set(Menu::getSort, menu.getSort());
            if (menu.getStatus() != null) wrapper.set(Menu::getStatus, menu.getStatus());
            if (menu.getVisible() != null) wrapper.set(Menu::getVisible, menu.getVisible());
            if (menu.getComponent() != null) wrapper.set(Menu::getComponent, menu.getComponent());
            if (menu.getKeepAlive() != null) wrapper.set(Menu::getKeepAlive, menu.getKeepAlive());
            if (menu.getEmbedded() != null) wrapper.set(Menu::getEmbedded, menu.getEmbedded());
            if (menu.getRedirect() != null) wrapper.set(Menu::getRedirect, menu.getRedirect());
            if (menu.getPermissions() != null) wrapper.set(Menu::getPermissions, menu.getPermissions());
            if (menu.getRemark() != null) wrapper.set(Menu::getRemark, menu.getRemark());
            // createBy、createTime、delFlag 不在普通更新中修改。

            int rows = menuMapper.update(null, wrapper);
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
            // 存在子菜单时禁止直接删除父菜单。
            List<Menu> children = listByParentId(menuId);
            if (!children.isEmpty()) {
                log.warn("Delete menu failed: menu has children, menuId={}", menuId);
                return false;
            }
            int rows = menuMapper.deleteById(menuId);
            return rows > 0;
        } catch (Exception e) {
            log.error("Delete menu failed: {}", e.getMessage());
            return false;
        }
    }
}
