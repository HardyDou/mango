package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.core.entity.AuthorizationAppModule;
import io.mango.authorization.core.entity.FrontendMenuRuntimeConfig;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
import io.mango.authorization.core.mapper.FrontendMenuRuntimeConfigMapper;
import io.mango.authorization.core.mapper.MenuMapper;
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

    private AppModuleServiceImpl appModuleService;

    @BeforeEach
    void setUp() {
        appModuleService = new AppModuleServiceImpl(appModuleMapper, menuMapper, menuRuntimeConfigMapper);
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
        verify(menuMapper, org.mockito.Mockito.times(3)).insert(menuCaptor.capture());
        List<Menu> menus = menuCaptor.getAllValues();
        Menu directory = menus.get(0);
        Menu page = menus.get(1);
        Menu button = menus.get(2);
        assertEquals("internal-admin", directory.getAppCode());
        assertEquals("guarantee", directory.getModuleCode());
        assertEquals(1, directory.getMenuType());
        assertEquals("guarantee", directory.getMenuCode());
        assertEquals(directory.getMenuId(), page.getParentId());
        assertEquals(2, page.getMenuType());
        assertEquals("guarantee:letter:list", page.getMenuCode());
        assertEquals("guarantee:letter:create,guarantee:letter:delete", page.getPermissions());
        assertEquals(page.getMenuId(), button.getParentId());
        assertEquals(3, button.getMenuType());
        assertEquals("guarantee:letter:create", button.getMenuCode());
        assertEquals("guarantee:letter:create", button.getPermissions());
        ArgumentCaptor<FrontendMenuRuntimeConfig> configCaptor =
                ArgumentCaptor.forClass(FrontendMenuRuntimeConfig.class);
        verify(menuRuntimeConfigMapper, org.mockito.Mockito.times(3)).insert(configCaptor.capture());
        assertEquals("LOCAL_ROUTE", configCaptor.getAllValues().get(1).getPageType());
        assertNotNull(configCaptor.getAllValues().get(1).getMenuId());
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
        manifest.setModuleCode("guarantee");
        manifest.setModuleName("保函模块");

        AppModuleResourceManifestCommand.Menu directory = new AppModuleResourceManifestCommand.Menu();
        directory.setMenuType(1);
        directory.setMenuName("保函管理");
        directory.setMenuCode("guarantee");
        directory.setPath("/guarantee");
        directory.setSort(10);

        AppModuleResourceManifestCommand.Menu page = new AppModuleResourceManifestCommand.Menu();
        page.setMenuType(2);
        page.setMenuName("保函列表");
        page.setMenuCode("guarantee:letter:list");
        page.setPath("/guarantee/letters");
        page.setComponent("guarantee/letter/index");
        page.setPermissions(List.of("guarantee:letter:create", "guarantee:letter:delete"));

        AppModuleResourceManifestCommand.Permission create = new AppModuleResourceManifestCommand.Permission();
        create.setPermissionCode("guarantee:letter:create");
        create.setPermissionName("新增保函");
        page.setPermissionItems(List.of(create));
        directory.setChildren(List.of(page));
        manifest.setMenus(List.of(directory));
        return manifest;
    }
}
