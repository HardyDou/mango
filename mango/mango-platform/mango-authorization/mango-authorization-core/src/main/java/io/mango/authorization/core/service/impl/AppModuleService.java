package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleMenuRequest;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.core.entity.AuthorizationAppModuleEntity;
import io.mango.authorization.core.entity.FrontendMenuRuntimeConfigEntity;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.entity.MenuPackageEntity;
import io.mango.authorization.core.entity.MenuPackageItemEntity;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleMenuEntity;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
import io.mango.authorization.core.mapper.FrontendMenuRuntimeConfigMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.MenuPackageItemMapper;
import io.mango.authorization.core.mapper.MenuPackageMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.service.IAppModuleService;
import io.mango.authorization.core.support.AuthorizationResourceIds;
import io.mango.common.exception.DependencyNotReadyException;
import io.mango.common.result.Require;
import io.mango.system.api.tenant.TenantPackageBindingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 逻辑应用集成模块服务实现。
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects managed mapper and service collaborators; copying them is not valid"))
public class AppModuleService implements IAppModuleService {

    private static final String PLATFORM_TENANT_ID = "default";
    private static final int BUTTON_MENU_TYPE = 3;

    private final AuthorizationAppModuleMapper appModuleMapper;
    private final MenuMapper menuMapper;
    private final FrontendMenuRuntimeConfigMapper menuRuntimeConfigMapper;
    private final MenuPackageMapper menuPackageMapper;
    private final MenuPackageItemMapper menuPackageItemMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final List<TenantPackageBindingProvider> tenantPackageBindingProviders;
    private final TenantMenuPackageBindingHandler tenantMenuPackageBindingHandler;

    @Override
    public List<AppModuleVO> list(String appCode, Integer status) {
        LambdaQueryWrapper<AuthorizationAppModuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(appCode), AuthorizationAppModuleEntity::getAppCode, appCode)
                .eq(status != null, AuthorizationAppModuleEntity::getStatus, status)
                .orderByAsc(AuthorizationAppModuleEntity::getSort)
                .orderByAsc(AuthorizationAppModuleEntity::getModuleCode);
        return appModuleMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AppModuleCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用模块命令不能为空");
        AuthorizationAppModuleEntity binding = find(command.getAppCode(), command.getModuleCode());
        boolean creating = binding == null;
        LocalDateTime now = LocalDateTime.now();
        if (creating) {
            binding = new AuthorizationAppModuleEntity();
            binding.setBindingId(command.getBindingId());
            binding.setTenantId(PLATFORM_TENANT_ID);
            binding.setAppCode(command.getAppCode());
            binding.setModuleCode(command.getModuleCode());
            binding.setCreateTime(now);
        }
        binding.setModuleName(resolveModuleName(command));
        binding.setStatus(command.getStatus() == null ? Integer.valueOf(1) : command.getStatus());
        binding.setSort(command.getSort() == null ? Integer.valueOf(0) : command.getSort());
        binding.setUpdateTime(now);
        if (creating) {
            appModuleMapper.insert(binding);
        } else {
            appModuleMapper.updateById(binding);
        }
        return binding.getBindingId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disable(String appCode, String moduleCode) {
        Require.notBlank(appCode, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用编码不能为空");
        Require.notBlank(moduleCode, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "模块编码不能为空");
        AuthorizationAppModuleEntity binding = find(appCode, moduleCode);
        return binding != null && disableBinding(binding);
    }

    @Override
    public Boolean disableRequired(String appCode, String moduleCode) {
        boolean disabled = Boolean.TRUE.equals(disable(appCode, moduleCode));
        Require.isTrue(disabled, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "应用集成模块不存在");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableByBindingId(Long bindingId) {
        Require.notNull(bindingId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用模块绑定ID不能为空");
        if (bindingId == null) {
            return false;
        }
        AuthorizationAppModuleEntity binding = appModuleMapper.selectById(bindingId);
        if (binding == null) {
            return false;
        }
        return disableBinding(binding);
    }

    @Override
    public Long findBindingId(String appCode, String moduleCode) {
        AuthorizationAppModuleEntity binding = find(appCode, moduleCode);
        return binding == null ? null : binding.getBindingId();
    }

    private Boolean disableBinding(AuthorizationAppModuleEntity binding) {
        binding.setStatus(0);
        binding.setUpdateTime(LocalDateTime.now());
        boolean changed = appModuleMapper.updateById(binding) > 0;
        List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                .eq(MenuEntity::getAppCode, binding.getAppCode())
                .eq(MenuEntity::getModuleCode, binding.getModuleCode())
                .eq(MenuEntity::getDelFlag, 0));
        List<Long> menuIds = menus.stream()
                .map(MenuEntity::getMenuId)
                .filter(id -> id != null)
                .toList();
        if (menuIds.isEmpty()) {
            return changed;
        }
        LocalDateTime now = LocalDateTime.now();
        for (MenuEntity menu : menus) {
            menu.setStatus(0);
            menu.setUpdateTime(now);
            changed = menuMapper.updateById(menu) > 0 || changed;
        }
        menuRuntimeConfigMapper.delete(new LambdaQueryWrapper<FrontendMenuRuntimeConfigEntity>()
                .in(FrontendMenuRuntimeConfigEntity::getMenuId, menuIds));
        menuPackageItemMapper.delete(new LambdaQueryWrapper<MenuPackageItemEntity>()
                .in(MenuPackageItemEntity::getMenuId, menuIds));
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuEntity>()
                .in(RoleMenuEntity::getMenuId, menuIds));
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer syncMenus(String appCode, String moduleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return 0;
        }
        List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                .eq(MenuEntity::getAppCode, appCode)
                .eq(MenuEntity::getModuleCode, moduleCode)
                .eq(MenuEntity::getDelFlag, 0));
        menus.forEach(this::ensureMenuRuntimeConfig);
        return menus.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer registerResourceManifest(AppModuleResourceManifestCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "资源清单不能为空");
        Require.notBlank(command.getAppCode(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "应用编码不能为空");
        Require.notBlank(command.getModuleCode(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "模块编码不能为空");
        validateManifestMenus(command.getMenus());
        ManifestContext context = new ManifestContext(command);
        validateManifestParents(context, command.getMenus());
        validateManifestPackages(context, command.getMenus());
        AppModuleCommand moduleCommand = toModuleCommand(command);
        save(moduleCommand);
        if (command.getMenus() == null || command.getMenus().isEmpty()) {
            return 0;
        }
        for (AppModuleMenuRequest menu : command.getMenus()) {
            context.increment(upsertManifestMenu(
                    context,
                    menu,
                    resolveManifestParentId(context, menu, 0L),
                    context.packageCodes(),
                    context.roleCodes()));
        }
        refreshTenantPackageBindings(context.changedPackageIds());
        return context.count();
    }

    private void validateManifestMenus(List<AppModuleMenuRequest> menus) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        for (AppModuleMenuRequest menu : menus) {
            if (menu == null) {
                continue;
            }
            Require.isTrue(menu.getPermissions() == null || menu.getPermissions().isEmpty(),
                    AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                    "AUTH_MENU 不再支持 permissions，请把权限码声明到菜单 apiCodes");
            Require.isTrue(menu.getPermissionItems() == null || menu.getPermissionItems().isEmpty(),
                    AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                    "AUTH_MENU 不再支持 permissionItems，请把权限码声明到菜单 apiCodes");
            validateManifestMenus(menu.getChildren());
        }
    }

    private void validateManifestPackages(ManifestContext context, List<AppModuleMenuRequest> menus) {
        Set<String> packageCodes = new LinkedHashSet<>();
        collectManifestPackageCodes(context, menus, null, packageCodes);
        List<String> missingPackageCodes = packageCodes.stream()
                .filter(packageCode -> findMenuPackage(context.appCode(), packageCode) == null)
                .toList();
        if (!missingPackageCodes.isEmpty()) {
            Require.rethrow(new DependencyNotReadyException(
                    "AUTH_MENU 依赖的菜单套餐尚未就绪："
                            + context.appCode() + "/" + String.join(",", missingPackageCodes)));
        }
    }

    private void validateManifestParents(ManifestContext context, List<AppModuleMenuRequest> menus) {
        Set<String> declaredMenuCodes = new LinkedHashSet<>();
        Set<String> parentCodes = new LinkedHashSet<>();
        collectManifestMenuCodes(menus, declaredMenuCodes, parentCodes);
        List<String> missingParentCodes = parentCodes.stream()
                .filter(parentCode -> !declaredMenuCodes.contains(parentCode))
                .filter(parentCode -> findManifestParentMenu(context.appCode(), parentCode) == null)
                .toList();
        if (!missingParentCodes.isEmpty()) {
            Require.rethrow(new DependencyNotReadyException(
                    "AUTH_MENU 依赖的父菜单尚未就绪："
                            + context.appCode() + "/" + String.join(",", missingParentCodes)));
        }
    }

    private void collectManifestMenuCodes(
            List<AppModuleMenuRequest> menus,
            Set<String> declaredMenuCodes,
            Set<String> parentCodes) {
        if (menus == null) {
            return;
        }
        for (AppModuleMenuRequest menu : menus) {
            if (menu == null) {
                continue;
            }
            String menuCode = StringUtils.hasText(menu.getMenuCode()) ? menu.getMenuCode().trim() : null;
            if (menuCode != null) {
                declaredMenuCodes.add(menuCode);
            }
            if (StringUtils.hasText(menu.getParentCode())) {
                String parentCode = menu.getParentCode().trim();
                Require.isTrue(!parentCode.equals(menuCode), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                        "资源清单父菜单不能指向自身：" + parentCode);
                parentCodes.add(parentCode);
            }
            collectManifestMenuCodes(menu.getChildren(), declaredMenuCodes, parentCodes);
        }
    }

    private void collectManifestPackageCodes(
            ManifestContext context,
            List<AppModuleMenuRequest> menus,
            List<String> inheritedPackageCodes,
            Set<String> packageCodes) {
        if (menus == null) {
            return;
        }
        for (AppModuleMenuRequest menu : menus) {
            if (menu == null) {
                continue;
            }
            List<String> resolvedPackageCodes = context.resolvePackageCodes(
                    menu.getPackageCodes(), inheritedPackageCodes);
            packageCodes.addAll(resolvedPackageCodes);
            collectManifestPackageCodes(context, menu.getChildren(), resolvedPackageCodes, packageCodes);
        }
    }

    private AppModuleCommand toModuleCommand(AppModuleResourceManifestCommand command) {
        AppModuleCommand moduleCommand = new AppModuleCommand();
        moduleCommand.setBindingId(AuthorizationResourceIds.stable(
                "authorization_app_module", PLATFORM_TENANT_ID,
                command.getAppCode(), command.getModuleCode()));
        moduleCommand.setAppCode(command.getAppCode());
        moduleCommand.setModuleCode(command.getModuleCode());
        moduleCommand.setModuleName(command.getModuleName());
        moduleCommand.setStatus(command.getStatus());
        moduleCommand.setSort(command.getSort());
        return moduleCommand;
    }

    private int upsertManifestMenu(
            ManifestContext context,
            AppModuleMenuRequest item,
            Long parentId,
            List<String> inheritedPackageCodes,
            List<String> inheritedRoleCodes) {
        if (item == null) {
            return 0;
        }
        Require.notBlank(item.getMenuName(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单名称不能为空");
        Require.notBlank(item.getMenuCode(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "菜单编码不能为空");
        List<String> packageCodes = context.resolvePackageCodes(item.getPackageCodes(), inheritedPackageCodes);
        List<String> roleCodes = context.resolveRoleCodes(item.getRoleCodes(), inheritedRoleCodes);
        MenuEntity menu = findMenu(context.appCode(), context.moduleCode(), item.getMenuCode());
        boolean creating = menu == null;
        LocalDateTime now = LocalDateTime.now();
        if (creating) {
            menu = new MenuEntity();
            menu.setMenuId(AuthorizationResourceIds.stable(
                    "authorization_menu", "1", context.appCode(),
                    context.moduleCode(), item.getMenuCode()));
            menu.setTenantId(1L);
            menu.setAppCode(context.appCode());
            menu.setModuleCode(context.moduleCode());
            menu.setMenuCode(item.getMenuCode());
            menu.setCreateTime(now);
        }
        fillManifestMenu(menu, item, parentId, context);
        menu.setUpdateTime(now);
        if (creating) {
            menuMapper.insert(menu);
        } else {
            menuMapper.updateById(menu);
        }
        saveRuntimeConfig(menu, item.getPageType(), item.getExternalUrl());
        assignManifestMenu(context, menu, packageCodes, roleCodes);
        int changed = 1;
        if (item.getChildren() != null) {
            for (AppModuleMenuRequest child : item.getChildren()) {
                changed += upsertManifestMenu(
                        context,
                        child,
                        resolveManifestParentId(context, child, menu.getMenuId()),
                        packageCodes,
                        roleCodes);
            }
        }
        return changed;
    }

    private Long resolveManifestParentId(
            ManifestContext context,
            AppModuleMenuRequest item,
            Long defaultParentId) {
        if (item == null || !StringUtils.hasText(item.getParentCode())) {
            return defaultParentId == null ? Long.valueOf(0L) : defaultParentId;
        }
        String parentCode = item.getParentCode().trim();
        Require.isTrue(!parentCode.equals(item.getMenuCode()), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR,
                "资源清单父菜单不能指向自身：" + parentCode);
        MenuEntity parent = findManifestParentMenu(context.appCode(), parentCode);
        if (parent == null) {
            return Require.rethrow(new DependencyNotReadyException(
                    "AUTH_MENU 依赖的父菜单尚未就绪：" + context.appCode() + "/" + parentCode));
        }
        return parent.getMenuId();
    }

    private void fillManifestMenu(
            MenuEntity menu,
            AppModuleMenuRequest item,
            Long parentId,
            ManifestContext context) {
        menu.setTenantId(1L);
        menu.setAppCode(context.appCode());
        menu.setModuleCode(context.moduleCode());
        menu.setParentId(parentId == null ? Long.valueOf(0L) : parentId);
        menu.setMenuType(item.getMenuType() == null ? Integer.valueOf(2) : item.getMenuType());
        menu.setMenuName(item.getMenuName());
        menu.setPath(item.getPath());
        menu.setIcon(item.getIcon());
        menu.setSort(item.getSort() == null ? Integer.valueOf(0) : item.getSort());
        menu.setStatus(item.getStatus() == null ? Integer.valueOf(1) : item.getStatus());
        menu.setVisible(item.getVisible() == null ? Integer.valueOf(1) : item.getVisible());
        menu.setComponent(item.getComponent());
        menu.setKeepAlive(item.getKeepAlive() == null ? Integer.valueOf(0) : item.getKeepAlive());
        menu.setEmbedded(item.getEmbedded() == null ? Integer.valueOf(0) : item.getEmbedded());
        menu.setRedirect(item.getRedirect());
        menu.setApiCodes(joinPermissions(item.getApiCodes()));
        menu.setRemark(item.getRemark());
        menu.setDelFlag(0);
    }

    private MenuEntity findMenu(String appCode, String moduleCode, String menuCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode) || !StringUtils.hasText(menuCode)) {
            return null;
        }
        return menuMapper.selectOne(new LambdaQueryWrapper<MenuEntity>()
                .eq(MenuEntity::getAppCode, appCode)
                .eq(MenuEntity::getModuleCode, moduleCode)
                .eq(MenuEntity::getMenuCode, menuCode)
                .last("LIMIT 1"));
    }

    private MenuEntity findManifestParentMenu(String appCode, String menuCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(menuCode)) {
            return null;
        }
        return menuMapper.selectOne(new LambdaQueryWrapper<MenuEntity>()
                .eq(MenuEntity::getTenantId, 1L)
                .eq(MenuEntity::getAppCode, appCode)
                .eq(MenuEntity::getMenuCode, menuCode)
                .in(MenuEntity::getMenuType, List.of(1, 2))
                .eq(MenuEntity::getDelFlag, 0)
                .last("LIMIT 1"));
    }

    private String joinPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return null;
        }
        List<String> values = permissions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        return values.isEmpty() ? null : String.join(",", values);
    }

    private AuthorizationAppModuleEntity find(String appCode, String moduleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return null;
        }
        return appModuleMapper.selectOne(new LambdaQueryWrapper<AuthorizationAppModuleEntity>()
                .eq(AuthorizationAppModuleEntity::getAppCode, appCode)
                .eq(AuthorizationAppModuleEntity::getModuleCode, moduleCode)
                .last("LIMIT 1"));
    }

    private String resolveModuleName(AppModuleCommand command) {
        if (StringUtils.hasText(command.getModuleName())) {
            return command.getModuleName();
        }
        return command.getModuleCode();
    }

    private void ensureMenuRuntimeConfig(MenuEntity menu) {
        saveRuntimeConfig(menu, defaultPageType(menu), null);
    }

    private void assignManifestMenu(
            ManifestContext context,
            MenuEntity menu,
            List<String> packageCodes,
            List<String> roleCodes) {
        assignMenuPackages(context, menu, packageCodes);
        assignRoleMenus(context, menu, roleCodes);
    }

    private void assignMenuPackages(ManifestContext context, MenuEntity menu, List<String> packageCodes) {
        List<String> missingPackageCodes = new ArrayList<>();
        for (String packageCode : packageCodes) {
            MenuPackageEntity menuPackage = findMenuPackage(context.appCode(), packageCode);
            if (menuPackage == null) {
                missingPackageCodes.add(packageCode);
                continue;
            }
            context.addChangedPackageId(menuPackage.getPackageId());
            MenuPackageItemEntity existing = menuPackageItemMapper.selectOne(new LambdaQueryWrapper<MenuPackageItemEntity>()
                    .eq(MenuPackageItemEntity::getPackageId, menuPackage.getPackageId())
                    .eq(MenuPackageItemEntity::getMenuId, menu.getMenuId())
                    .last("LIMIT 1"));
            if (existing != null) {
                continue;
            }
            MenuPackageItemEntity item = new MenuPackageItemEntity();
            item.setId(AuthorizationResourceIds.stable(
                    "authorization_menu_package_item", String.valueOf(menuPackage.getTenantId()),
                    String.valueOf(menuPackage.getPackageId()), String.valueOf(menu.getMenuId())));
            item.setTenantId(menuPackage.getTenantId());
            item.setPackageId(menuPackage.getPackageId());
            item.setMenuId(menu.getMenuId());
            item.setSort(menu.getSort());
            menuPackageItemMapper.insert(item);
        }
        if (!missingPackageCodes.isEmpty()) {
            Require.rethrow(new DependencyNotReadyException(
                    "AUTH_MENU 依赖的菜单套餐尚未就绪："
                            + context.appCode() + "/" + String.join(",", missingPackageCodes)));
        }
    }

    private void refreshTenantPackageBindings(Set<Long> packageIds) {
        for (Long packageId : packageIds) {
            Set<Long> tenantIds = tenantPackageBindingProviders.stream()
                    .map(provider -> provider.listTenantIdsByPackage(packageId))
                    .filter(ids -> ids != null && !ids.isEmpty())
                    .flatMap(List::stream)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (tenantIds.isEmpty()) {
                continue;
            }
            tenantIds.forEach(tenantId -> tenantMenuPackageBindingHandler.bindPackage(tenantId, packageId));
        }
    }

    private void assignRoleMenus(ManifestContext context, MenuEntity menu, List<String> roleCodes) {
        for (String roleCode : roleCodes) {
            RoleEntity role = findRole(context.appCode(), roleCode);
            if (role == null) {
                continue;
            }
            RoleMenuEntity existing = roleMenuMapper.selectOne(new LambdaQueryWrapper<RoleMenuEntity>()
                    .eq(RoleMenuEntity::getRoleId, role.getRoleId())
                    .eq(RoleMenuEntity::getMenuId, menu.getMenuId())
                    .last("LIMIT 1"));
            if (existing != null) {
                continue;
            }
            RoleMenuEntity roleMenu = new RoleMenuEntity();
            roleMenu.setId(AuthorizationResourceIds.stable(
                    "authorization_role_menu", String.valueOf(role.getTenantId()),
                    String.valueOf(role.getRoleId()), String.valueOf(menu.getMenuId())));
            roleMenu.setTenantId(role.getTenantId());
            roleMenu.setRoleId(role.getRoleId());
            roleMenu.setMenuId(menu.getMenuId());
            roleMenuMapper.insert(roleMenu);
        }
    }

    private MenuPackageEntity findMenuPackage(String appCode, String packageCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(packageCode)) {
            return null;
        }
        return menuPackageMapper.selectOne(new LambdaQueryWrapper<MenuPackageEntity>()
                .eq(MenuPackageEntity::getAppCode, appCode)
                .eq(MenuPackageEntity::getPackageCode, packageCode)
                .last("LIMIT 1"));
    }

    private RoleEntity findRole(String appCode, String roleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(roleCode)) {
            return null;
        }
        return roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getAppCode, appCode)
                .eq(RoleEntity::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }

    private void saveRuntimeConfig(MenuEntity menu, String pageType, String externalUrl) {
        FrontendMenuRuntimeConfigEntity config = menuRuntimeConfigMapper.selectOne(
                new LambdaQueryWrapper<FrontendMenuRuntimeConfigEntity>()
                        .eq(FrontendMenuRuntimeConfigEntity::getMenuId, menu.getMenuId())
                        .last("LIMIT 1"));
        boolean creating = config == null;
        LocalDateTime now = LocalDateTime.now();
        if (creating) {
            config = new FrontendMenuRuntimeConfigEntity();
            config.setConfigId(AuthorizationResourceIds.stable(
                    "frontend_menu_runtime_config", PLATFORM_TENANT_ID,
                    String.valueOf(menu.getMenuId())));
            config.setTenantId(PLATFORM_TENANT_ID);
            config.setMenuId(menu.getMenuId());
            config.setCreateTime(now);
        }
        config.setAppCode(menu.getAppCode());
        config.setPageType(StringUtils.hasText(pageType) ? pageType : defaultPageType(menu));
        config.setExternalUrl(externalUrl);
        config.setUpdateTime(now);
        if (creating) {
            menuRuntimeConfigMapper.insert(config);
        } else {
            menuRuntimeConfigMapper.updateById(config);
        }
    }

    private String defaultPageType(MenuEntity menu) {
        if (Integer.valueOf(BUTTON_MENU_TYPE).equals(menu.getMenuType())) {
            return "BUTTON";
        }
        if (Integer.valueOf(1).equals(menu.getEmbedded())) {
            return "IFRAME";
        }
        return "LOCAL_ROUTE";
    }

    private AppModuleVO toVO(AuthorizationAppModuleEntity binding) {
        AppModuleVO vo = new AppModuleVO();
        vo.setBindingId(binding.getBindingId());
        vo.setAppCode(binding.getAppCode());
        vo.setModuleCode(binding.getModuleCode());
        vo.setModuleName(binding.getModuleName());
        vo.setStatus(binding.getStatus());
        vo.setSort(binding.getSort());
        vo.setCreateTime(binding.getCreateTime());
        vo.setUpdateTime(binding.getUpdateTime());
        return vo;
    }

    private static final class ManifestContext {

        private final String appCode;
        private final String moduleCode;
        private final List<String> packageCodes;
        private final List<String> roleCodes;
        private final Set<Long> changedPackageIds = new LinkedHashSet<>();
        private int count;

        private ManifestContext(AppModuleResourceManifestCommand command) {
            this.appCode = command.getAppCode();
            this.moduleCode = command.getModuleCode();
            this.packageCodes = cleanCodes(command.getPackageCodes());
            this.roleCodes = cleanCodes(command.getRoleCodes());
        }

        private String appCode() {
            return appCode;
        }

        private String moduleCode() {
            return moduleCode;
        }

        private List<String> packageCodes() {
            return packageCodes;
        }

        private List<String> roleCodes() {
            return roleCodes;
        }

        private List<String> resolvePackageCodes(List<String> menuPackageCodes, List<String> inheritedPackageCodes) {
            if (menuPackageCodes == null) {
                return inheritedPackageCodes == null ? packageCodes : inheritedPackageCodes;
            }
            return cleanCodes(menuPackageCodes);
        }

        private List<String> resolveRoleCodes(List<String> menuRoleCodes, List<String> inheritedRoleCodes) {
            if (menuRoleCodes == null) {
                return inheritedRoleCodes == null ? roleCodes : inheritedRoleCodes;
            }
            return cleanCodes(menuRoleCodes);
        }

        private void increment(int value) {
            count += value;
        }

        private int count() {
            return count;
        }

        private void addChangedPackageId(Long packageId) {
            if (packageId != null) {
                changedPackageIds.add(packageId);
            }
        }

        private Set<Long> changedPackageIds() {
            return changedPackageIds;
        }

        private static List<String> cleanCodes(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            return values.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
        }
    }
}
