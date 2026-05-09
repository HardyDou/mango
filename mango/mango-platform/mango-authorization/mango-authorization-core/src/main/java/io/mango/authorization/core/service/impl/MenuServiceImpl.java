package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IMenuService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务实现。
 */
@Slf4j
@Service("permissionMenuServiceImpl")
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

    private final MenuMapper menuMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final ISubjectAuthorityService subjectAuthorityService;

    @Override
    public List<MenuVO> listMenus(String appCode, Integer type, Long parentId, String menuName, Integer status, boolean tree) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(appCode != null && !appCode.isBlank(), Menu::getAppCode, appCode)
                .eq(parentId != null, Menu::getParentId, parentId)
                .eq(type != null, Menu::getMenuType, type)
                .eq(status != null, Menu::getStatus, status)
                .like(StringUtils.hasText(menuName), Menu::getMenuName, menuName)
                .orderByAsc(Menu::getSort);
        List<Menu> menus = menuMapper.selectList(wrapper);
        return toMenuResult(menus, tree);
    }

    @Override
    public List<MenuVO> listUserMenus(String appCode, Integer type, Long parentId, AuthorizationQuery query, boolean tree) {
        if (query == null || query.subjectId() == null) {
            return new ArrayList<>();
        }

        String effectiveAppCode = StringUtils.hasText(appCode) ? appCode : query.systemCode();
        AuthorizationQuery scopedQuery = query.withSystemCode(effectiveAppCode);
        List<Menu> visibleMenus = listVisibleMenus(effectiveAppCode);
        if (visibleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        List<Menu> scopedMenus;
        if (hasAllMenuAccess(scopedQuery)) {
            scopedMenus = visibleMenus;
        } else {
            Set<Long> authorizedMenuIds = resolveAuthorizedMenuIds(scopedQuery, visibleMenus);
            if (authorizedMenuIds.isEmpty()) {
                return new ArrayList<>();
            }
            scopedMenus = visibleMenus.stream()
                    .filter(menu -> authorizedMenuIds.contains(menu.getMenuId()))
                    .collect(Collectors.toList());
        }

        List<Menu> filteredMenus = applyUserMenuFilter(scopedMenus, type, parentId, tree);
        return toMenuResult(filteredMenus, tree);
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
        Map<Long, List<Menu>> childrenByParentId = menus.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        Set<Long> menuIds = menus.stream()
                .map(Menu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
        return menus.stream()
                .filter(menu -> menu.getParentId() == null
                        || menu.getParentId() == 0
                        || !menuIds.contains(menu.getParentId()))
                .map(menu -> buildMenuNode(menu, childrenByParentId))
                .collect(Collectors.toList());
    }

    private MenuVO buildMenuNode(Menu menu, Map<Long, List<Menu>> childrenByParentId) {
        MenuVO vo = convertToMenuVO(menu);
        List<MenuVO> children = childrenByParentId.getOrDefault(menu.getMenuId(), List.of())
                .stream()
                .map(child -> buildMenuNode(child, childrenByParentId))
                .collect(Collectors.toList());
        if (!children.isEmpty()) {
            vo.setChildren(children);
        }
        return vo;
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

    private List<MenuVO> toMenuResult(List<Menu> menus, boolean tree) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        if (tree) {
            return buildMenuTree(menus);
        }
        return menus.stream()
                .map(this::convertToMenuVO)
                .collect(Collectors.toList());
    }

    private List<Menu> listVisibleMenus(String appCode) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(appCode), Menu::getAppCode, appCode)
                .eq(Menu::getStatus, 1)
                .eq(Menu::getVisible, 1)
                .orderByAsc(Menu::getSort);
        return menuMapper.selectList(wrapper);
    }

    private boolean hasAllMenuAccess(AuthorizationQuery query) {
        return subjectAuthorityService.listSubjectPermissions(query)
                .stream()
                .anyMatch("*:*"::equals);
    }

    private Set<Long> resolveAuthorizedMenuIds(AuthorizationQuery query, List<Menu> visibleMenus) {
        List<Long> roleIds = listSubjectRoleIds(query);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        LambdaQueryWrapper<RoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenu::getRoleId, roleIds);
        Set<Long> directlyAssignedMenuIds = roleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (directlyAssignedMenuIds.isEmpty()) {
            return Collections.emptySet();
        }

        Map<Long, Menu> menuById = visibleMenus.stream()
                .collect(Collectors.toMap(Menu::getMenuId, menu -> menu));
        Set<Long> authorizedIds = new LinkedHashSet<>();
        for (Long menuId : directlyAssignedMenuIds) {
            collectMenuWithAncestors(menuId, menuById, authorizedIds);
        }
        return authorizedIds;
    }

    private List<Long> listSubjectRoleIds(AuthorizationQuery query) {
        Long tenantId = parseTenantId(query.tenantId());
        LambdaQueryWrapper<SubjectRoleBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBinding::getSubjectId, query.subjectId())
                .eq(SubjectRoleBinding::getSubjectType, query.subjectType())
                .eq(tenantId != null, SubjectRoleBinding::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), SubjectRoleBinding::getAppCode, query.systemCode())
                .eq(StringUtils.hasText(query.realm()), SubjectRoleBinding::getRealm, query.realm())
                .eq(StringUtils.hasText(query.actorType()), SubjectRoleBinding::getActorType, query.actorType())
                .eq(StringUtils.hasText(query.partyType()), SubjectRoleBinding::getPartyType, query.partyType())
                .eq(query.partyId() != null, SubjectRoleBinding::getPartyId, query.partyId());
        return subjectRoleBindingMapper.selectList(wrapper)
                .stream()
                .map(SubjectRoleBinding::getRoleId)
                .collect(Collectors.toList());
    }

    private Long parseTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void collectMenuWithAncestors(Long menuId, Map<Long, Menu> menuById, Set<Long> collector) {
        Long currentId = menuId;
        while (currentId != null && currentId > 0 && collector.add(currentId)) {
            Menu menu = menuById.get(currentId);
            if (menu == null) {
                break;
            }
            currentId = menu.getParentId();
        }
    }

    private List<Menu> applyUserMenuFilter(List<Menu> menus, Integer type, Long parentId, boolean tree) {
        StreamContext context = new StreamContext(menus);
        List<Menu> filtered = context.sortedMenus.stream()
                .filter(menu -> type == null || type.equals(menu.getMenuType()))
                .collect(Collectors.toList());

        if (parentId == null || parentId == 0) {
            return filtered;
        }

        if (!tree) {
            return filtered.stream()
                    .filter(menu -> parentId.equals(menu.getParentId()))
                    .collect(Collectors.toList());
        }

        Set<Long> subtreeIds = new LinkedHashSet<>();
        collectDescendantIds(parentId, context.childrenByParentId, subtreeIds);
        return filtered.stream()
                .filter(menu -> subtreeIds.contains(menu.getMenuId()))
                .collect(Collectors.toList());
    }

    private void collectDescendantIds(Long parentId, Map<Long, List<Menu>> childrenByParentId, Set<Long> collector) {
        List<Menu> children = childrenByParentId.getOrDefault(parentId, List.of());
        for (Menu child : children) {
            if (collector.add(child.getMenuId())) {
                collectDescendantIds(child.getMenuId(), childrenByParentId, collector);
            }
        }
    }

    private static final class StreamContext {
        private final List<Menu> sortedMenus;
        private final Map<Long, List<Menu>> childrenByParentId;

        private StreamContext(List<Menu> menus) {
            this.sortedMenus = menus.stream()
                    .sorted(Comparator.comparing(Menu::getSort, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(Menu::getMenuId, Comparator.nullsLast(Long::compareTo)))
                    .collect(Collectors.toList());
            this.childrenByParentId = this.sortedMenus.stream()
                    .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        }
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
        fillCreateDefaults(menu);
        int rows = menuMapper.insert(menu);
        return rows > 0;
    }

    private void fillCreateDefaults(Menu menu) {
        if (!StringUtils.hasText(menu.getAppCode())) {
            menu.setAppCode("internal-admin");
        }
        if (menu.getTenantId() == null) {
            menu.setTenantId(1L);
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getMenuType() == null) {
            menu.setMenuType(2);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getKeepAlive() == null) {
            menu.setKeepAlive(0);
        }
        if (menu.getEmbedded() == null) {
            menu.setEmbedded(0);
        }
        if (menu.getDelFlag() == null) {
            menu.setDelFlag(0);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(Long menuId, Menu menu) {
        if (menuId == null || menu == null) {
            log.warn("Update menu failed: invalid parameters");
            return false;
        }
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long menuId) {
        if (menuId == null) {
            log.warn("Delete menu failed: menuId is null");
            return false;
        }
        // 存在子菜单时禁止直接删除父菜单。
        List<Menu> children = listByParentId(menuId);
        if (!children.isEmpty()) {
            log.warn("Delete menu failed: menu has children, menuId={}", menuId);
            return false;
        }
        int rows = menuMapper.deleteById(menuId);
        return rows > 0;
    }
}
