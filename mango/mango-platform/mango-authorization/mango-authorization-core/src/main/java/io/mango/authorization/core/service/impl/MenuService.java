package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.MenuCommand;
import io.mango.authorization.api.query.MenuTreeQuery;
import io.mango.authorization.core.entity.AuthorizationAppModuleEntity;
import io.mango.authorization.core.entity.FrontendMenuRuntimeConfigEntity;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleMenuEntity;
import io.mango.authorization.core.entity.SubjectRoleBindingEntity;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.api.vo.MenuMetaVO;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
import io.mango.authorization.core.mapper.FrontendMenuRuntimeConfigMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IMenuService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
public class MenuService implements IMenuService {

    private final MenuMapper menuMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final FrontendMenuRuntimeConfigMapper frontendMenuRuntimeConfigMapper;
    private final AuthorizationAppModuleMapper appModuleMapper;
    private final RoleMapper roleMapper;
    private final ISubjectAuthorityService subjectAuthorityService;

    @Override
    public List<MenuVO> listMenus(MenuTreeQuery query) {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getAppCode()), MenuEntity::getAppCode, query.getAppCode())
                .eq(StringUtils.hasText(query.getModuleCode()), MenuEntity::getModuleCode, query.getModuleCode())
                .eq(query.getParentId() != null, MenuEntity::getParentId, query.getParentId())
                .eq(query.getType() != null, MenuEntity::getMenuType, query.getType())
                .eq(query.getStatus() != null, MenuEntity::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getMenuName()), MenuEntity::getMenuName, query.getMenuName())
                .orderByAsc(MenuEntity::getSort);
        List<MenuEntity> menus = menuMapper.selectList(wrapper);
        return toMenuResult(menus, isTreeFormat(query));
    }

    @Override
    public List<MenuVO> listUserMenus(MenuTreeQuery query, AuthorizationQuery authorizationQuery) {
        if (authorizationQuery == null || authorizationQuery.subjectId() == null) {
            return new ArrayList<>();
        }

        boolean tree = isTreeFormat(query);
        String effectiveAppCode = StringUtils.hasText(query.getAppCode())
                ? query.getAppCode() : authorizationQuery.systemCode();
        AuthorizationQuery scopedQuery = authorizationQuery.withSystemCode(effectiveAppCode);
        List<MenuEntity> enabledMenus = listEnabledMenus(effectiveAppCode);
        if (enabledMenus.isEmpty()) {
            return new ArrayList<>();
        }

        List<MenuEntity> scopedMenus;
        if (hasAllMenuAccess(scopedQuery)) {
            scopedMenus = enabledMenus;
        } else {
            Set<Long> authorizedMenuIds = resolveAuthorizedMenuIds(scopedQuery, enabledMenus);
            if (authorizedMenuIds.isEmpty()) {
                return new ArrayList<>();
            }
            scopedMenus = enabledMenus.stream()
                    .filter(menu -> authorizedMenuIds.contains(menu.getMenuId()))
                    .collect(Collectors.toList());
        }

        List<MenuEntity> filteredMenus = applyUserMenuFilter(
                scopedMenus, query.getType(), query.getParentId(), tree);
        return toMenuResult(filteredMenus, tree);
    }

    private boolean isTreeFormat(MenuTreeQuery query) {
        return "tree".equalsIgnoreCase(query.getFmt());
    }

    @Override
    public MenuEntity getById(Long menuId) {
        MenuEntity menu = menuMapper.selectById(menuId);
        applyRuntimeConfig(menu);
        return menu;
    }

    @Override
    public MenuVO getMenu(Long menuId) {
        Require.notNull(menuId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单ID不能为空");
        MenuEntity menu = getById(menuId);
        Require.notNull(menu, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "菜单不存在");
        return convertToMenuVO(menu);
    }

    @Override
    public Void createMenu(MenuCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单命令不能为空");
        Require.isTrue(addMenu(toEntity(command)), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                "添加菜单失败");
        return null;
    }

    @Override
    public Void updateMenu(MenuCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单命令不能为空");
        Require.notNull(command.getMenuId(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单ID不能为空");
        Require.isTrue(updateMenu(command.getMenuId(), toEntity(command)),
                AuthorizationCode.AUTHORIZATION_NOT_FOUND, "更新菜单失败");
        return null;
    }

    @Override
    public Void removeMenu(Long menuId) {
        Require.isTrue(deleteMenu(menuId), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                "删除菜单失败，菜单可能不存在或仍有子菜单");
        return null;
    }

    @Override
    public List<MenuEntity> listByParentId(Long parentId) {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuEntity::getParentId, parentId)
                .orderByAsc(MenuEntity::getSort);
        return menuMapper.selectList(wrapper);
    }

    /**
     * 基于内存递归组装菜单树，避免 N+1 查询。
     */
    @Override
    public List<MenuVO> buildMenuTree(List<MenuEntity> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        applyRuntimeConfig(menus);
        Map<Long, List<MenuEntity>> childrenByParentId = menus.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        Set<Long> menuIds = menus.stream()
                .map(MenuEntity::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
        return menus.stream()
                .filter(menu -> menu.getParentId() == null
                        || menu.getParentId() == 0
                        || !menuIds.contains(menu.getParentId()))
                .map(menu -> buildMenuNode(menu, childrenByParentId))
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> listAllPermissionCodes() {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuEntity::getStatus, 1)
                .orderByAsc(MenuEntity::getSort);
        return menuMapper.selectList(wrapper)
                .stream()
                .flatMap(menu -> permissionCodes(menu).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> permissionCodes(MenuEntity menu) {
        if (menu == null) {
            return new ArrayList<>();
        }
        if (StringUtils.hasText(menu.getApiCodes())) {
            return Arrays.stream(menu.getApiCodes().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private MenuVO buildMenuNode(MenuEntity menu, Map<Long, List<MenuEntity>> childrenByParentId) {
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

    private MenuVO convertToMenuVO(MenuEntity menu) {
        MenuVO vo = new MenuVO();
        vo.setMenuId(menu.getMenuId());
        vo.setAppCode(menu.getAppCode());
        vo.setModuleCode(menu.getModuleCode());
        vo.setParentId(menu.getParentId());
        vo.setMenuType(menu.getMenuType());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuCode(menu.getMenuCode());
        vo.setPath(menu.getPath());
        vo.setPageType(menu.getPageType());
        vo.setComponent(menu.getComponent());
        vo.setExternalUrl(menu.getExternalUrl());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setStatus(menu.getStatus());
        vo.setVisible(menu.getVisible());
        vo.setKeepAlive(menu.getKeepAlive());
        vo.setEmbedded(menu.getEmbedded());
        vo.setRedirect(menu.getRedirect());
        vo.setPermissions(menu.getPermissions());
        vo.setApiCodes(menu.getApiCodes());
        vo.setButtonType(menu.getButtonType());
        vo.setButtonDisplayRule(menu.getButtonDisplayRule());
        vo.setCreateBy(menu.getCreateBy());
        vo.setUpdateBy(menu.getUpdateBy());
        vo.setCreateTime(menu.getCreateTime());
        vo.setUpdateTime(menu.getUpdateTime());
        vo.setRemark(menu.getRemark());

        // 构造前端路由元信息。
        MenuMetaVO meta = new MenuMetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        meta.setLink(menu.getExternalUrl());
        meta.setFrameSrc(menu.getExternalUrl());
        meta.setIsFrame("IFRAME".equals(menu.getPageType()));
        meta.setIsLink("EXTERNAL_LINK".equals(menu.getPageType()));
        vo.setMeta(meta);

        return vo;
    }

    private MenuEntity toEntity(MenuCommand command) {
        MenuEntity entity = new MenuEntity();
        entity.setMenuId(command.getMenuId());
        entity.setAppCode(command.getAppCode());
        entity.setModuleCode(command.getModuleCode());
        entity.setParentId(command.getParentId());
        entity.setMenuType(command.getMenuType());
        entity.setMenuName(command.getMenuName());
        entity.setMenuCode(command.getMenuCode());
        entity.setPath(command.getPath());
        entity.setPageType(command.getPageType());
        entity.setExternalUrl(command.getExternalUrl());
        entity.setIcon(command.getIcon());
        entity.setSort(command.getSort());
        entity.setStatus(command.getStatus());
        entity.setVisible(command.getVisible());
        entity.setComponent(command.getComponent());
        entity.setKeepAlive(command.getKeepAlive());
        entity.setEmbedded(command.getEmbedded());
        entity.setRedirect(command.getRedirect());
        entity.setPermissions(command.getPermissions());
        entity.setApiCodes(command.getApiCodes());
        entity.setButtonType(command.getButtonType());
        entity.setButtonDisplayRule(command.getButtonDisplayRule());
        entity.setCreateBy(command.getCreateBy());
        entity.setUpdateBy(command.getUpdateBy());
        entity.setCreateTime(command.getCreateTime());
        entity.setUpdateTime(command.getUpdateTime());
        entity.setRemark(command.getRemark());
        entity.setDelFlag(command.getDelFlag());
        return entity;
    }

    private List<MenuVO> toMenuResult(List<MenuEntity> menus, boolean tree) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        applyRuntimeConfig(menus);
        if (tree) {
            return buildMenuTree(menus);
        }
        return menus.stream()
                .map(this::convertToMenuVO)
                .collect(Collectors.toList());
    }

    private List<MenuEntity> listEnabledMenus(String appCode) {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(appCode), MenuEntity::getAppCode, appCode)
                .eq(MenuEntity::getStatus, 1)
                .in(MenuEntity::getMenuType, List.of(1, 2))
                .orderByAsc(MenuEntity::getSort);
        List<String> enabledModuleCodes = listEnabledModuleCodes(appCode);
        if (!enabledModuleCodes.isEmpty()) {
            wrapper.in(MenuEntity::getModuleCode, enabledModuleCodes);
        }
        return menuMapper.selectList(wrapper);
    }

    private List<String> listEnabledModuleCodes(String appCode) {
        if (!StringUtils.hasText(appCode)) {
            return List.of();
        }
        return appModuleMapper.selectList(new LambdaQueryWrapper<AuthorizationAppModuleEntity>()
                        .eq(AuthorizationAppModuleEntity::getAppCode, appCode)
                        .eq(AuthorizationAppModuleEntity::getStatus, 1))
                .stream()
                .map(AuthorizationAppModuleEntity::getModuleCode)
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean hasAllMenuAccess(AuthorizationQuery query) {
        return subjectAuthorityService.listSubjectPermissions(query)
                .stream()
                .anyMatch("*:*"::equals);
    }

    private Set<Long> resolveAuthorizedMenuIds(AuthorizationQuery query, List<MenuEntity> visibleMenus) {
        List<Long> roleIds = listSubjectRoleIds(query);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        LambdaQueryWrapper<RoleMenuEntity> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenuEntity::getRoleId, roleIds);
        Set<Long> directlyAssignedMenuIds = roleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(RoleMenuEntity::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (directlyAssignedMenuIds.isEmpty()) {
            return Collections.emptySet();
        }

        Map<Long, MenuEntity> menuById = visibleMenus.stream()
                .collect(Collectors.toMap(MenuEntity::getMenuId, menu -> menu));
        Set<Long> authorizedIds = new LinkedHashSet<>();
        for (Long menuId : directlyAssignedMenuIds) {
            MenuEntity menu = menuById.get(menuId);
            if (menu == null || Integer.valueOf(0).equals(menu.getVisible())) {
                continue;
            }
            collectMenuWithAncestors(menuId, menuById, authorizedIds);
        }
        return authorizedIds;
    }

    private List<Long> listSubjectRoleIds(AuthorizationQuery query) {
        Long tenantId = parseTenantId(query.tenantId());
        if (StringUtils.hasText(query.tenantId()) && tenantId == null) {
            return new ArrayList<>();
        }
        Set<Long> roleIds = new LinkedHashSet<>();
        if (AuthorizationQuery.SUBJECT_TYPE_ANONYMOUS.equals(query.subjectType())) {
            roleIds.addAll(listDefaultRoleIds(query, tenantId));
            return new ArrayList<>(roleIds);
        }
        LambdaQueryWrapper<SubjectRoleBindingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBindingEntity::getSubjectId, query.subjectId())
                .eq(SubjectRoleBindingEntity::getSubjectType, query.subjectType())
                .eq(tenantId != null, SubjectRoleBindingEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), SubjectRoleBindingEntity::getAppCode, query.systemCode())
                .eq(StringUtils.hasText(query.realm()), SubjectRoleBindingEntity::getRealm, query.realm())
                .eq(StringUtils.hasText(query.actorType()), SubjectRoleBindingEntity::getActorType, query.actorType())
                .eq(StringUtils.hasText(query.partyType()), SubjectRoleBindingEntity::getPartyType, query.partyType())
                .eq(query.partyId() != null, SubjectRoleBindingEntity::getPartyId, query.partyId());
        roleIds.addAll(subjectRoleBindingMapper.selectList(wrapper)
                .stream()
                .map(SubjectRoleBindingEntity::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        roleIds.addAll(listDefaultRoleIds(query, tenantId));
        return new ArrayList<>(roleIds);
    }

    private List<Long> listDefaultRoleIds(AuthorizationQuery query, Long tenantId) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        String roleCode = AuthorizationQuery.SUBJECT_TYPE_ANONYMOUS.equals(query.subjectType())
                ? SubjectAuthorityService.ROLE_ANONYMOUS
                : SubjectAuthorityService.ROLE_LOGIN;
        wrapper.eq(RoleEntity::getRoleCode, roleCode)
                .eq(RoleEntity::getStatus, 1)
                .eq(tenantId != null, RoleEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), RoleEntity::getAppCode, query.systemCode());
        return roleMapper.selectList(wrapper)
                .stream()
                .map(RoleEntity::getRoleId)
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

    private void collectMenuWithAncestors(Long menuId, Map<Long, MenuEntity> menuById, Set<Long> collector) {
        Long currentId = menuId;
        while (currentId != null && currentId > 0 && collector.add(currentId)) {
            MenuEntity menu = menuById.get(currentId);
            if (menu == null) {
                break;
            }
            currentId = menu.getParentId();
        }
    }

    private List<MenuEntity> applyUserMenuFilter(List<MenuEntity> menus, Integer type, Long parentId, boolean tree) {
        StreamContext context = new StreamContext(menus);
        List<MenuEntity> filtered = context.sortedMenus.stream()
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

    private void collectDescendantIds(Long parentId, Map<Long, List<MenuEntity>> childrenByParentId, Set<Long> collector) {
        List<MenuEntity> children = childrenByParentId.getOrDefault(parentId, List.of());
        for (MenuEntity child : children) {
            if (collector.add(child.getMenuId())) {
                collectDescendantIds(child.getMenuId(), childrenByParentId, collector);
            }
        }
    }

    private static final class StreamContext {
        private final List<MenuEntity> sortedMenus;
        private final Map<Long, List<MenuEntity>> childrenByParentId;

        private StreamContext(List<MenuEntity> menus) {
            this.sortedMenus = menus.stream()
                    .sorted(Comparator.comparing(MenuEntity::getSort, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(MenuEntity::getMenuId, Comparator.nullsLast(Long::compareTo)))
                    .collect(Collectors.toList());
            this.childrenByParentId = this.sortedMenus.stream()
                    .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        }

        private static String firstText(String... values) {
            if (values == null) {
                return null;
            }
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMenu(MenuEntity menu) {
        Require.notNull(menu, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单不能为空");
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
        if (rows > 0) {
            saveRuntimeConfig(menu);
        }
        return rows > 0;
    }

    private void fillCreateDefaults(MenuEntity menu) {
        if (!StringUtils.hasText(menu.getAppCode())) {
            menu.setAppCode("internal-admin");
        }
        if (!StringUtils.hasText(menu.getModuleCode())) {
            menu.setModuleCode(resolveDefaultModuleCode(menu));
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
    public boolean updateMenu(Long menuId, MenuEntity menu) {
        Require.notNull(menuId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单ID不能为空");
        if (menuId == null || menu == null) {
            log.warn("Update menu failed: invalid parameters");
            return false;
        }
        MenuEntity existing = menuMapper.selectById(menuId);
        if (existing == null) {
            log.warn("Update menu failed: menu not found, menuId={}", menuId);
            return false;
        }
        // 只更新非空字段。
        LambdaUpdateWrapper<MenuEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MenuEntity::getId, menuId);
        if (menu.getAppCode() != null) wrapper.set(MenuEntity::getAppCode, menu.getAppCode());
        if (menu.getModuleCode() != null) wrapper.set(MenuEntity::getModuleCode, menu.getModuleCode());
        if (menu.getParentId() != null) wrapper.set(MenuEntity::getParentId, menu.getParentId());
        if (menu.getMenuType() != null) wrapper.set(MenuEntity::getMenuType, menu.getMenuType());
        if (menu.getMenuName() != null) wrapper.set(MenuEntity::getMenuName, menu.getMenuName());
        if (menu.getMenuCode() != null) wrapper.set(MenuEntity::getMenuCode, menu.getMenuCode());
        if (menu.getPath() != null) wrapper.set(MenuEntity::getPath, menu.getPath());
        if (menu.getIcon() != null) wrapper.set(MenuEntity::getIcon, menu.getIcon());
        if (menu.getSort() != null) wrapper.set(MenuEntity::getSort, menu.getSort());
        if (menu.getStatus() != null) wrapper.set(MenuEntity::getStatus, menu.getStatus());
        if (menu.getVisible() != null) wrapper.set(MenuEntity::getVisible, menu.getVisible());
        if (menu.getComponent() != null) wrapper.set(MenuEntity::getComponent, menu.getComponent());
        if (menu.getKeepAlive() != null) wrapper.set(MenuEntity::getKeepAlive, menu.getKeepAlive());
        if (menu.getEmbedded() != null) wrapper.set(MenuEntity::getEmbedded, menu.getEmbedded());
        if (menu.getRedirect() != null) wrapper.set(MenuEntity::getRedirect, menu.getRedirect());
        if (menu.getPermissions() != null) wrapper.set(MenuEntity::getPermissions, menu.getPermissions());
        if (menu.getApiCodes() != null) wrapper.set(MenuEntity::getApiCodes, menu.getApiCodes());
        if (menu.getButtonType() != null) wrapper.set(MenuEntity::getButtonType, menu.getButtonType());
        if (menu.getButtonDisplayRule() != null) wrapper.set(MenuEntity::getButtonDisplayRule, menu.getButtonDisplayRule());
        if (menu.getRemark() != null) wrapper.set(MenuEntity::getRemark, menu.getRemark());
        // createBy、createTime、delFlag 不在普通更新中修改。

        int rows = menuMapper.update(null, wrapper);
        if (rows > 0) {
            menu.setMenuId(menuId);
            if (!StringUtils.hasText(menu.getAppCode())) {
                menu.setAppCode(existing.getAppCode());
            }
            saveRuntimeConfig(menu);
        }
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long menuId) {
        Require.notNull(menuId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单ID不能为空");
        if (menuId == null) {
            log.warn("Delete menu failed: menuId is null");
            return false;
        }
        // 存在子菜单时禁止直接删除父菜单。
        List<MenuEntity> children = listByParentId(menuId);
        if (!children.isEmpty()) {
            log.warn("Delete menu failed: menu has children, menuId={}", menuId);
            return false;
        }
        int rows = menuMapper.deleteById(menuId);
        if (rows > 0) {
            frontendMenuRuntimeConfigMapper.delete(new LambdaQueryWrapper<FrontendMenuRuntimeConfigEntity>()
                    .eq(FrontendMenuRuntimeConfigEntity::getMenuId, menuId));
        }
        return rows > 0;
    }

    private void saveRuntimeConfig(MenuEntity menu) {
        if (menu == null || menu.getMenuId() == null) {
            return;
        }
        FrontendMenuRuntimeConfigEntity config = frontendMenuRuntimeConfigMapper.selectOne(
                new LambdaQueryWrapper<FrontendMenuRuntimeConfigEntity>()
                        .eq(FrontendMenuRuntimeConfigEntity::getMenuId, menu.getMenuId())
                        .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (config == null) {
            config = new FrontendMenuRuntimeConfigEntity();
            config.setMenuId(menu.getMenuId());
            config.setCreateTime(now);
        }
        config.setAppCode(defaultString(menu.getAppCode(), "internal-admin"));
        String pageType = normalizePageType(menu);
        config.setPageType(pageType);
        config.setExternalUrl(isExternalPageType(pageType) ? menu.getExternalUrl() : null);
        config.setUpdateTime(now);
        if (config.getConfigId() == null) {
            frontendMenuRuntimeConfigMapper.insert(config);
        } else {
            frontendMenuRuntimeConfigMapper.updateById(config);
        }
    }

    private void applyRuntimeConfig(MenuEntity menu) {
        if (menu == null || menu.getMenuId() == null) {
            return;
        }
        FrontendMenuRuntimeConfigEntity config = frontendMenuRuntimeConfigMapper.selectOne(
                new LambdaQueryWrapper<FrontendMenuRuntimeConfigEntity>()
                        .eq(FrontendMenuRuntimeConfigEntity::getMenuId, menu.getMenuId())
                        .last("LIMIT 1"));
        applyRuntimeConfig(menu, config);
    }

    private void applyRuntimeConfig(List<MenuEntity> menus) {
        List<Long> menuIds = menus.stream()
                .map(MenuEntity::getMenuId)
                .filter(id -> id != null)
                .toList();
        if (menuIds.isEmpty()) {
            return;
        }
        Map<Long, FrontendMenuRuntimeConfigEntity> configByMenuId = frontendMenuRuntimeConfigMapper.selectList(
                        new LambdaQueryWrapper<FrontendMenuRuntimeConfigEntity>()
                                .in(FrontendMenuRuntimeConfigEntity::getMenuId, menuIds))
                .stream()
                .collect(Collectors.toMap(FrontendMenuRuntimeConfigEntity::getMenuId, item -> item, (left, right) -> left));
        menus.forEach(menu -> applyRuntimeConfig(menu, configByMenuId.get(menu.getMenuId())));
    }

    private void applyRuntimeConfig(MenuEntity menu, FrontendMenuRuntimeConfigEntity config) {
        menu.setPageType(defaultString(config == null ? null : config.getPageType(), defaultPageType(menu)));
        menu.setExternalUrl(config == null ? null : config.getExternalUrl());
    }

    private String normalizePageType(MenuEntity menu) {
        if (menu != null && Integer.valueOf(3).equals(menu.getMenuType())) {
            return "BUTTON";
        }
        String pageType = menu == null ? null : menu.getPageType();
        if ("MICRO_ROUTE".equals(pageType) || "IFRAME".equals(pageType) || "EXTERNAL_LINK".equals(pageType)) {
            return pageType;
        }
        return "LOCAL_ROUTE";
    }

    private boolean isExternalPageType(String pageType) {
        return "IFRAME".equals(pageType) || "EXTERNAL_LINK".equals(pageType);
    }

    private String defaultPageType(MenuEntity menu) {
        if (menu != null && Integer.valueOf(3).equals(menu.getMenuType())) {
            return "BUTTON";
        }
        if (menu != null && Integer.valueOf(1).equals(menu.getEmbedded())) {
            return "IFRAME";
        }
        return "LOCAL_ROUTE";
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String resolveDefaultModuleCode(MenuEntity menu) {
        String code = menu == null ? null : menu.getMenuCode();
        String path = menu == null ? null : menu.getPath();
        String source = StreamContext.firstText(code, path);
        if (!StringUtils.hasText(source)) {
            return "mango-system";
        }
        if (source.startsWith("authorization:")) {
            return "mango-authorization";
        }
        if (source.startsWith("workflow:") || source.startsWith("/workflow")) {
            return "mango-workflow";
        }
        return "mango-system";
    }
}
