package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppModuleServiceImpl Tests")
class AppModuleServiceImplTest {

    @Mock
    private AuthorizationAppModuleMapper appModuleMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private FrontendMenuRuntimeConfigMapper menuRuntimeConfigMapper;
    @Mock
    private MenuPackageMapper menuPackageMapper;
    @Mock
    private MenuPackageItemMapper menuPackageItemMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;

    private AppModuleServiceImpl appModuleService;

    @BeforeEach
    void setUp() {
        appModuleService = new AppModuleServiceImpl(
                appModuleMapper,
                menuMapper,
                menuRuntimeConfigMapper,
                menuPackageMapper,
                menuPackageItemMapper,
                roleMapper,
                roleMenuMapper);
    }

    @Test
    @DisplayName("registerResourceManifest should upsert menu tree and permission buttons")
    void registerResourceManifest_menuTreeAndPermissions_upsertsMenus() {
        AtomicLong ids = new AtomicLong(100);
        when(appModuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(appModuleMapper.insert(any(AuthorizationAppModule.class))).thenAnswer(invocation -> {
            AuthorizationAppModule module = invocation.getArgument(0);
            module.setBindingId(1L);
            return 1;
        });
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(menuMapper.insert(any(Menu.class))).thenAnswer(invocation -> {
            Menu menu = invocation.getArgument(0);
            menu.setMenuId(ids.incrementAndGet());
            return 1;
        });
        when(menuRuntimeConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(menuRuntimeConfigMapper.insert(any(FrontendMenuRuntimeConfig.class))).thenReturn(1);

        int registered = appModuleService.registerResourceManifest(createManifest());

        assertEquals(3, registered);
        ArgumentCaptor<Menu> menuCaptor = ArgumentCaptor.forClass(Menu.class);
        verify(menuMapper, times(3)).insert(menuCaptor.capture());
        List<Menu> menus = menuCaptor.getAllValues();
        Menu directory = menus.get(0);
        Menu page = menus.get(1);
        Menu button = menus.get(2);
        assertEquals("internal-admin", directory.getAppCode());
        assertEquals("contract", directory.getModuleCode());
        assertEquals(1, directory.getMenuType());
        assertEquals("contract", directory.getMenuCode());
        assertEquals(directory.getMenuId(), page.getParentId());
        assertEquals(2, page.getMenuType());
        assertEquals("contract:archive:list", page.getMenuCode());
        assertEquals("contract:archive:create,contract:archive:delete", page.getPermissions());
        assertEquals(page.getMenuId(), button.getParentId());
        assertEquals(3, button.getMenuType());
        assertEquals("contract:archive:create", button.getMenuCode());
        assertEquals("contract:archive:create", button.getPermissions());
        ArgumentCaptor<FrontendMenuRuntimeConfig> configCaptor =
                ArgumentCaptor.forClass(FrontendMenuRuntimeConfig.class);
        verify(menuRuntimeConfigMapper, times(3)).insert(configCaptor.capture());
        assertEquals("LOCAL_ROUTE", configCaptor.getAllValues().get(1).getPageType());
        assertNotNull(configCaptor.getAllValues().get(1).getMenuId());
    }

    @Test
    @DisplayName("registerResourceManifest should assign menus to packages and default roles")
    void registerResourceManifest_packageAndRoleCodes_assignsMenus() {
        AtomicLong ids = new AtomicLong(200);
        when(appModuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(appModuleMapper.insert(any(AuthorizationAppModule.class))).thenReturn(1);
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(menuMapper.insert(any(Menu.class))).thenAnswer(invocation -> {
            Menu menu = invocation.getArgument(0);
            menu.setMenuId(ids.incrementAndGet());
            return 1;
        });
        when(menuRuntimeConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(menuRuntimeConfigMapper.insert(any(FrontendMenuRuntimeConfig.class))).thenReturn(1);
        MenuPackage menuPackage = new MenuPackage();
        menuPackage.setPackageId(1L);
        menuPackage.setPackageCode("internal-admin-default");
        when(menuPackageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(menuPackage);
        when(menuPackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(menuPackageItemMapper.insert(any(MenuPackageItem.class))).thenReturn(1);
        Role role = new Role();
        role.setTenantId(1L);
        role.setRoleId(1L);
        role.setRoleCode("ROLE_ADMIN");
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(role);
        when(roleMenuMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(roleMenuMapper.insert(any(RoleMenu.class))).thenReturn(1);

        AppModuleResourceManifestCommand manifest = createManifest();
        manifest.setPackageCodes(List.of("internal-admin-default"));
        manifest.setRoleCodes(List.of("ROLE_ADMIN"));

        int registered = appModuleService.registerResourceManifest(manifest);

        assertEquals(3, registered);
        ArgumentCaptor<MenuPackageItem> packageItemCaptor = ArgumentCaptor.forClass(MenuPackageItem.class);
        verify(menuPackageItemMapper, times(3)).insert(packageItemCaptor.capture());
        assertEquals(1L, packageItemCaptor.getAllValues().get(0).getPackageId());
        assertEquals(201L, packageItemCaptor.getAllValues().get(0).getMenuId());
        ArgumentCaptor<RoleMenu> roleMenuCaptor = ArgumentCaptor.forClass(RoleMenu.class);
        verify(roleMenuMapper, times(3)).insert(roleMenuCaptor.capture());
        assertEquals(1L, roleMenuCaptor.getAllValues().get(0).getTenantId());
        assertEquals(1L, roleMenuCaptor.getAllValues().get(0).getRoleId());
        assertEquals(201L, roleMenuCaptor.getAllValues().get(0).getMenuId());
    }

    @Test
    @DisplayName("registerResourceManifest should attach root menu to declared parent code")
    void registerResourceManifest_parentCode_attachesRootMenuToExistingParent() {
        AtomicLong ids = new AtomicLong(300);
        Menu platform = new Menu();
        platform.setMenuId(2700L);
        platform.setAppCode("internal-admin");
        platform.setModuleCode("mango-calendar");
        platform.setMenuCode("data");
        platform.setMenuType(1);
        when(appModuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(appModuleMapper.insert(any(AuthorizationAppModule.class))).thenReturn(1);
        when(menuMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(platform)
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(null);
        when(menuMapper.insert(any(Menu.class))).thenAnswer(invocation -> {
            Menu menu = invocation.getArgument(0);
            menu.setMenuId(ids.incrementAndGet());
            return 1;
        });
        when(menuRuntimeConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(menuRuntimeConfigMapper.insert(any(FrontendMenuRuntimeConfig.class))).thenReturn(1);

        AppModuleResourceManifestCommand manifest = createManifest();
        manifest.getMenus().get(0).setParentCode("data");

        int registered = appModuleService.registerResourceManifest(manifest);

        assertEquals(3, registered);
        ArgumentCaptor<Menu> menuCaptor = ArgumentCaptor.forClass(Menu.class);
        verify(menuMapper, times(3)).insert(menuCaptor.capture());
        List<Menu> menus = menuCaptor.getAllValues();
        assertEquals(2700L, menus.get(0).getParentId());
        assertEquals(menus.get(0).getMenuId(), menus.get(1).getParentId());
        assertEquals(menus.get(1).getMenuId(), menus.get(2).getParentId());
    }

    @Test
    @DisplayName("registerResourceManifest should only save module when no menus")
    void registerResourceManifest_emptyMenus_savesModuleOnly() {
        when(appModuleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(appModuleMapper.insert(any(AuthorizationAppModule.class))).thenReturn(1);
        AppModuleResourceManifestCommand manifest = new AppModuleResourceManifestCommand();
        manifest.setAppCode("internal-admin");
        manifest.setModuleCode("empty-module");

        int registered = appModuleService.registerResourceManifest(manifest);

        assertEquals(0, registered);
        verify(menuMapper, never()).insert(any(Menu.class));
    }

    private AppModuleResourceManifestCommand createManifest() {
        AppModuleResourceManifestCommand manifest = new AppModuleResourceManifestCommand();
        manifest.setAppCode("internal-admin");
        manifest.setModuleCode("contract");
        manifest.setModuleName("合同模块");

        AppModuleResourceManifestCommand.Menu directory = new AppModuleResourceManifestCommand.Menu();
        directory.setMenuType(1);
        directory.setMenuName("合同管理");
        directory.setMenuCode("contract");
        directory.setPath("/contract");
        directory.setSort(10);

        AppModuleResourceManifestCommand.Menu page = new AppModuleResourceManifestCommand.Menu();
        page.setMenuType(2);
        page.setMenuName("合同列表");
        page.setMenuCode("contract:archive:list");
        page.setPath("/contract/archives");
        page.setComponent("contract/archive/index");
        page.setPermissions(List.of("contract:archive:create", "contract:archive:delete"));

        AppModuleResourceManifestCommand.Permission create = new AppModuleResourceManifestCommand.Permission();
        create.setPermissionCode("contract:archive:create");
        create.setPermissionName("新增合同");
        page.setPermissionItems(List.of(create));
        directory.setChildren(List.of(page));
        manifest.setMenus(List.of(directory));
        return manifest;
    }
}
