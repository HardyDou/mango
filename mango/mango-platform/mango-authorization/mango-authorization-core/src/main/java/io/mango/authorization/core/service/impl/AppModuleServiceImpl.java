package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.AppModuleCommand;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.api.vo.AppModuleVO;
import io.mango.authorization.core.entity.AuthorizationAppModule;
import io.mango.authorization.core.entity.FrontendMenuRuntimeConfig;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.MenuPackage;
import io.mango.authorization.core.entity.MenuPackageItem;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
import io.mango.authorization.core.mapper.FrontendMenuRuntimeConfigMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.MenuPackageItemMapper;
import io.mango.authorization.core.mapper.MenuPackageMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.service.IAppModuleService;
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
@RequiredArgsConstructor
public class AppModuleServiceImpl implements IAppModuleService {

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
        LambdaQueryWrapper<AuthorizationAppModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(appCode), AuthorizationAppModule::getAppCode, appCode)
                .eq(status != null, AuthorizationAppModule::getStatus, status)
                .orderByAsc(AuthorizationAppModule::getSort)
                .orderByAsc(AuthorizationAppModule::getModuleCode);
        return appModuleMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AppModuleCommand command) {
        AuthorizationAppModule binding = find(command.getAppCode(), command.getModuleCode());
        LocalDateTime now = LocalDateTime.now();
        if (binding == null) {
            binding = new AuthorizationAppModule();
            binding.setAppCode(command.getAppCode());
            binding.setModuleCode(command.getModuleCode());
            binding.setCreateTime(now);
        }
        binding.setModuleName(resolveModuleName(command));
        binding.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        binding.setSort(command.getSort() == null ? 0 : command.getSort());
        binding.setUpdateTime(now);
        if (binding.getBindingId() == null) {
            appModuleMapper.insert(binding);
        } else {
            appModuleMapper.updateById(binding);
        }
        return binding.getBindingId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disable(String appCode, String moduleCode) {
        AuthorizationAppModule binding = find(appCode, moduleCode);
        if (binding == null) {
            return false;
        }
        binding.setStatus(0);
        binding.setUpdateTime(LocalDateTime.now());
        return appModuleMapper.updateById(binding) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer syncMenus(String appCode, String moduleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return 0;
        }
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getAppCode, appCode)
                .eq(Menu::getModuleCode, moduleCode)
                .eq(Menu::getDelFlag, 0));
        menus.forEach(this::ensureMenuRuntimeConfig);
        return menus.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer registerResourceManifest(AppModuleResourceManifestCommand command) {
        Require.notNull(command, "资源清单不能为空");
        Require.notBlank(command.getAppCode(), "应用编码不能为空");
        Require.notBlank(command.getModuleCode(), "模块编码不能为空");
        AppModuleCommand moduleCommand = toModuleCommand(command);
        save(moduleCommand);
        if (command.getMenus() == null || command.getMenus().isEmpty()) {
            return 0;
        }
        ManifestContext context = new ManifestContext(command);
        for (AppModuleResourceManifestCommand.Menu menu : command.getMenus()) {
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

    private AppModuleCommand toModuleCommand(AppModuleResourceManifestCommand command) {
        AppModuleCommand moduleCommand = new AppModuleCommand();
        moduleCommand.setAppCode(command.getAppCode());
        moduleCommand.setModuleCode(command.getModuleCode());
        moduleCommand.setModuleName(command.getModuleName());
        moduleCommand.setStatus(command.getStatus());
        moduleCommand.setSort(command.getSort());
        return moduleCommand;
    }

    private int upsertManifestMenu(
            ManifestContext context,
            AppModuleResourceManifestCommand.Menu item,
            Long parentId,
            List<String> inheritedPackageCodes,
            List<String> inheritedRoleCodes) {
        if (item == null) {
            return 0;
        }
        Require.notBlank(item.getMenuName(), "菜单名称不能为空");
        Require.notBlank(item.getMenuCode(), "菜单编码不能为空");
        List<String> packageCodes = context.resolvePackageCodes(item.getPackageCodes(), inheritedPackageCodes);
        List<String> roleCodes = context.resolveRoleCodes(item.getRoleCodes(), inheritedRoleCodes);
        Menu menu = findMenu(context.appCode(), context.moduleCode(), item.getMenuCode());
        LocalDateTime now = LocalDateTime.now();
        if (menu == null) {
            menu = new Menu();
            menu.setAppCode(context.appCode());
            menu.setModuleCode(context.moduleCode());
            menu.setMenuCode(item.getMenuCode());
            menu.setCreateTime(now);
        }
        fillManifestMenu(menu, item, parentId, context);
        menu.setUpdateTime(now);
        if (menu.getMenuId() == null) {
            menuMapper.insert(menu);
        } else {
            menuMapper.updateById(menu);
        }
        saveRuntimeConfig(menu, item.getPageType(), item.getExternalUrl());
        assignManifestMenu(context, menu, packageCodes, roleCodes);
        int changed = 1;
        changed += upsertPermissionMenus(context, item, menu.getMenuId(), packageCodes, roleCodes);
        if (item.getChildren() != null) {
            for (AppModuleResourceManifestCommand.Menu child : item.getChildren()) {
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
            AppModuleResourceManifestCommand.Menu item,
            Long defaultParentId) {
        if (item == null || !StringUtils.hasText(item.getParentCode())) {
            return defaultParentId == null ? 0L : defaultParentId;
        }
        String parentCode = item.getParentCode().trim();
        Menu parent = findManifestParentMenu(context.appCode(), parentCode);
        Require.notNull(parent, "资源清单父菜单不存在：" + parentCode);
        Require.isTrue(!parentCode.equals(item.getMenuCode()), "资源清单父菜单不能指向自身：" + parentCode);
        return parent.getMenuId();
    }

    private void fillManifestMenu(
            Menu menu,
            AppModuleResourceManifestCommand.Menu item,
            Long parentId,
            ManifestContext context) {
        menu.setTenantId(1L);
        menu.setAppCode(context.appCode());
        menu.setModuleCode(context.moduleCode());
        menu.setParentId(parentId == null ? 0L : parentId);
        menu.setMenuType(item.getMenuType() == null ? 2 : item.getMenuType());
        menu.setMenuName(item.getMenuName());
        menu.setPath(item.getPath());
        menu.setIcon(item.getIcon());
        menu.setSort(item.getSort() == null ? 0 : item.getSort());
        menu.setStatus(item.getStatus() == null ? 1 : item.getStatus());
        menu.setVisible(item.getVisible() == null ? 1 : item.getVisible());
        menu.setComponent(item.getComponent());
        menu.setKeepAlive(item.getKeepAlive() == null ? 0 : item.getKeepAlive());
        menu.setEmbedded(item.getEmbedded() == null ? 0 : item.getEmbedded());
        menu.setRedirect(item.getRedirect());
        menu.setPermissions(joinPermissions(item.getPermissions()));
        menu.setRemark(item.getRemark());
        menu.setDelFlag(0);
    }

    private int upsertPermissionMenus(
            ManifestContext context,
            AppModuleResourceManifestCommand.Menu item,
            Long parentId,
            List<String> inheritedPackageCodes,
            List<String> inheritedRoleCodes) {
        if (item.getPermissionItems() == null || item.getPermissionItems().isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (AppModuleResourceManifestCommand.Permission permission : item.getPermissionItems()) {
            if (permission == null) {
                continue;
            }
            Require.notBlank(permission.getPermissionCode(), "权限编码不能为空");
            Require.notBlank(permission.getPermissionName(), "权限名称不能为空");
            Menu menu = findMenu(context.appCode(), context.moduleCode(), permissionMenuCode(permission));
            LocalDateTime now = LocalDateTime.now();
            if (menu == null) {
                menu = new Menu();
                menu.setAppCode(context.appCode());
                menu.setModuleCode(context.moduleCode());
                menu.setMenuCode(permission.getPermissionCode());
                menu.setCreateTime(now);
            }
            fillPermissionMenu(menu, permission, parentId, context);
            menu.setUpdateTime(now);
            if (menu.getMenuId() == null) {
                menuMapper.insert(menu);
            } else {
                menuMapper.updateById(menu);
            }
            ensureMenuRuntimeConfig(menu);
            assignManifestMenu(
                    context,
                    menu,
                    context.resolvePackageCodes(permission.getPackageCodes(), inheritedPackageCodes),
                    context.resolveRoleCodes(permission.getRoleCodes(), inheritedRoleCodes));
            changed++;
        }
        return changed;
    }

    private void fillPermissionMenu(
            Menu menu,
            AppModuleResourceManifestCommand.Permission permission,
            Long parentId,
            ManifestContext context) {
        menu.setTenantId(1L);
        menu.setAppCode(context.appCode());
        menu.setModuleCode(context.moduleCode());
        menu.setParentId(parentId == null ? 0L : parentId);
        menu.setMenuType(3);
        menu.setMenuName(permission.getPermissionName());
        menu.setMenuCode(permissionMenuCode(permission));
        menu.setPath(null);
        menu.setIcon(null);
        menu.setSort(permission.getSort() == null ? 0 : permission.getSort());
        menu.setStatus(permission.getStatus() == null ? 1 : permission.getStatus());
        menu.setVisible(0);
        menu.setComponent(null);
        menu.setKeepAlive(0);
        menu.setEmbedded(0);
        menu.setRedirect(null);
        menu.setPermissions(permission.getPermissionCode());
        menu.setRemark(permission.getRemark());
        menu.setDelFlag(0);
    }

    private String permissionMenuCode(AppModuleResourceManifestCommand.Permission permission) {
        if (StringUtils.hasText(permission.getMenuCode())) {
            return permission.getMenuCode().trim();
        }
        return permission.getPermissionCode().trim();
    }

    private Menu findMenu(String appCode, String moduleCode, String menuCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode) || !StringUtils.hasText(menuCode)) {
            return null;
        }
        return menuMapper.selectOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getAppCode, appCode)
                .eq(Menu::getModuleCode, moduleCode)
                .eq(Menu::getMenuCode, menuCode)
                .last("LIMIT 1"));
    }

    private Menu findManifestParentMenu(String appCode, String menuCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(menuCode)) {
            return null;
        }
        return menuMapper.selectOne(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getTenantId, 1L)
                .eq(Menu::getAppCode, appCode)
                .eq(Menu::getMenuCode, menuCode)
                .in(Menu::getMenuType, List.of(1, 2))
                .eq(Menu::getDelFlag, 0)
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

    private AuthorizationAppModule find(String appCode, String moduleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(moduleCode)) {
            return null;
        }
        return appModuleMapper.selectOne(new LambdaQueryWrapper<AuthorizationAppModule>()
                .eq(AuthorizationAppModule::getAppCode, appCode)
                .eq(AuthorizationAppModule::getModuleCode, moduleCode)
                .last("LIMIT 1"));
    }

    private String resolveModuleName(AppModuleCommand command) {
        if (StringUtils.hasText(command.getModuleName())) {
            return command.getModuleName();
        }
        return command.getModuleCode();
    }

    private void ensureMenuRuntimeConfig(Menu menu) {
        saveRuntimeConfig(menu, defaultPageType(menu), null);
    }

    private void assignManifestMenu(
            ManifestContext context,
            Menu menu,
            List<String> packageCodes,
            List<String> roleCodes) {
        assignMenuPackages(context, menu, packageCodes);
        assignRoleMenus(context, menu, roleCodes);
    }

    private void assignMenuPackages(ManifestContext context, Menu menu, List<String> packageCodes) {
        for (String packageCode : packageCodes) {
            MenuPackage menuPackage = findMenuPackage(context.appCode(), packageCode);
            if (menuPackage == null) {
                continue;
            }
            context.addChangedPackageId(menuPackage.getPackageId());
            MenuPackageItem existing = menuPackageItemMapper.selectOne(new LambdaQueryWrapper<MenuPackageItem>()
                    .eq(MenuPackageItem::getPackageId, menuPackage.getPackageId())
                    .eq(MenuPackageItem::getMenuId, menu.getMenuId())
                    .last("LIMIT 1"));
            if (existing != null) {
                continue;
            }
            MenuPackageItem item = new MenuPackageItem();
            item.setPackageId(menuPackage.getPackageId());
            item.setMenuId(menu.getMenuId());
            item.setSort(menu.getSort());
            menuPackageItemMapper.insert(item);
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

    private void assignRoleMenus(ManifestContext context, Menu menu, List<String> roleCodes) {
        for (String roleCode : roleCodes) {
            Role role = findRole(context.appCode(), roleCode);
            if (role == null) {
                continue;
            }
            RoleMenu existing = roleMenuMapper.selectOne(new LambdaQueryWrapper<RoleMenu>()
                    .eq(RoleMenu::getRoleId, role.getRoleId())
                    .eq(RoleMenu::getMenuId, menu.getMenuId())
                    .last("LIMIT 1"));
            if (existing != null) {
                continue;
            }
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setTenantId(role.getTenantId());
            roleMenu.setRoleId(role.getRoleId());
            roleMenu.setMenuId(menu.getMenuId());
            roleMenuMapper.insert(roleMenu);
        }
    }

    private MenuPackage findMenuPackage(String appCode, String packageCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(packageCode)) {
            return null;
        }
        return menuPackageMapper.selectOne(new LambdaQueryWrapper<MenuPackage>()
                .eq(MenuPackage::getAppCode, appCode)
                .eq(MenuPackage::getPackageCode, packageCode)
                .last("LIMIT 1"));
    }

    private Role findRole(String appCode, String roleCode) {
        if (!StringUtils.hasText(appCode) || !StringUtils.hasText(roleCode)) {
            return null;
        }
        return roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getAppCode, appCode)
                .eq(Role::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }

    private void saveRuntimeConfig(Menu menu, String pageType, String externalUrl) {
        FrontendMenuRuntimeConfig config = menuRuntimeConfigMapper.selectOne(
                new LambdaQueryWrapper<FrontendMenuRuntimeConfig>()
                        .eq(FrontendMenuRuntimeConfig::getMenuId, menu.getMenuId())
                        .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (config == null) {
            config = new FrontendMenuRuntimeConfig();
            config.setMenuId(menu.getMenuId());
            config.setCreateTime(now);
        }
        config.setAppCode(menu.getAppCode());
        config.setPageType(StringUtils.hasText(pageType) ? pageType : defaultPageType(menu));
        config.setExternalUrl(externalUrl);
        config.setUpdateTime(now);
        if (config.getConfigId() == null) {
            menuRuntimeConfigMapper.insert(config);
        } else {
            menuRuntimeConfigMapper.updateById(config);
        }
    }

    private String defaultPageType(Menu menu) {
        if (Integer.valueOf(3).equals(menu.getMenuType())) {
            return "BUTTON";
        }
        if (Integer.valueOf(1).equals(menu.getEmbedded())) {
            return "IFRAME";
        }
        return "LOCAL_ROUTE";
    }

    private AppModuleVO toVO(AuthorizationAppModule binding) {
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
