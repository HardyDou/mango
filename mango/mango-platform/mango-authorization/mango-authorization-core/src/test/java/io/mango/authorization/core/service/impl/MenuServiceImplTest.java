package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.entity.FrontendMenuRuntimeConfig;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.mapper.FrontendMenuRuntimeConfigMapper;
import io.mango.authorization.core.mapper.AuthorizationAppModuleMapper;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IMenuService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MenuServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MenuServiceImpl Tests")
class MenuServiceImplTest {

    @Mock
    private MenuMapper menuMapper;
    @Mock
    private SubjectRoleBindingMapper subjectRoleBindingMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private FrontendMenuRuntimeConfigMapper frontendMenuRuntimeConfigMapper;
    @Mock
    private AuthorizationAppModuleMapper appModuleMapper;
    @Mock
    private ISubjectAuthorityService subjectAuthorityService;

    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl(
                menuMapper,
                subjectRoleBindingMapper,
                roleMenuMapper,
                frontendMenuRuntimeConfigMapper,
                appModuleMapper,
                subjectAuthorityService);
    }

    @Test
    @DisplayName("getById should return menu when exists")
    void getById_existingMenu_returnsMenu() {
        Menu menu = createMenu(1L, "Test Menu", "test:menu");
        FrontendMenuRuntimeConfig config = new FrontendMenuRuntimeConfig();
        config.setMenuId(1L);
        config.setPageType("IFRAME");
        config.setExternalUrl("https://example.com/frame");
        when(menuMapper.selectById(1L)).thenReturn(menu);
        when(frontendMenuRuntimeConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        Menu result = menuService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMenuId());
        assertEquals("IFRAME", result.getPageType());
        assertEquals("https://example.com/frame", result.getExternalUrl());
    }

    @Test
    @DisplayName("listByParentId should return menus for parent")
    void listByParentId_existingParent_returnsMenus() {
        Menu menu1 = createMenu(1L, "Menu 1", "menu:1");
        Menu menu2 = createMenu(2L, "Menu 2", "menu:2");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(menu1, menu2));

        List<Menu> result = menuService.listByParentId(0L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("buildMenuTree should return empty list for null input")
    void buildMenuTree_nullInput_returnsEmptyList() {
        List<MenuVO> result = menuService.buildMenuTree(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("buildMenuTree should return empty list for empty input")
    void buildMenuTree_emptyInput_returnsEmptyList() {
        List<MenuVO> result = menuService.buildMenuTree(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listUserMenus should keep hidden enabled menus for route registration")
    void listUserMenus_hiddenEnabledMenu_returnsHiddenRouteNode() {
        Menu root = createMenu(10L, "Root", "root");
        root.setAppCode("internal-admin");
        root.setModuleCode("mango-notice");
        root.setVisible(1);
        Menu visibleMenu = createMenu(11L, "Visible", "visible");
        visibleMenu.setAppCode("internal-admin");
        visibleMenu.setModuleCode("mango-notice");
        visibleMenu.setParentId(10L);
        visibleMenu.setMenuType(2);
        visibleMenu.setVisible(1);
        Menu hiddenMenu = createMenu(12L, "Hidden", "hidden");
        hiddenMenu.setAppCode("internal-admin");
        hiddenMenu.setModuleCode("mango-notice");
        hiddenMenu.setParentId(10L);
        hiddenMenu.setMenuType(2);
        hiddenMenu.setVisible(0);
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, visibleMenu, hiddenMenu));
        when(subjectAuthorityService.listSubjectPermissions(any(AuthorizationQuery.class))).thenReturn(List.of("*:*"));
        when(frontendMenuRuntimeConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<MenuVO> result = menuService.listUserMenus(
                "internal-admin",
                null,
                null,
                AuthorizationQuery.user(1L),
                true);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getChildren().size());
        assertTrue(result.get(0).getChildren().stream()
                .anyMatch(menu -> Long.valueOf(12L).equals(menu.getMenuId()) && Integer.valueOf(0).equals(menu.getVisible())));
    }

    @Test
    @DisplayName("buildMenuTree should keep workflow manage children under workflow root")
    void buildMenuTree_workflowManageMenuExists_keepsManageChildrenNested() {
        Menu workflow = createMenu(26L, "审批中心", "workflow");
        workflow.setSort(2);
        Menu task = createMenu(2601L, "流程办理", "workflow:task");
        task.setParentId(26L);
        task.setSort(1);
        Menu manage = createMenu(2604L, "流程管理", "workflow:manage");
        manage.setParentId(26L);
        manage.setSort(3);
        Menu template = createMenu(260401L, "流程模板", "workflow:template");
        template.setParentId(2604L);
        template.setMenuType(2);
        template.setSort(1);
        Menu definition = createMenu(260402L, "流程定义", "workflow:definition");
        definition.setParentId(2604L);
        definition.setMenuType(2);
        definition.setSort(2);
        when(frontendMenuRuntimeConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<MenuVO> result = menuService.buildMenuTree(List.of(workflow, task, manage, template, definition));

        assertEquals(1, result.size());
        MenuVO workflowNode = result.get(0);
        assertEquals(26L, workflowNode.getMenuId());
        assertEquals(2, workflowNode.getChildren().size());
        MenuVO manageNode = workflowNode.getChildren().stream()
                .filter(menu -> Long.valueOf(2604L).equals(menu.getMenuId()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, manageNode.getChildren().size());
        assertTrue(manageNode.getChildren().stream()
                .anyMatch(menu -> Long.valueOf(260401L).equals(menu.getMenuId())));
        assertTrue(manageNode.getChildren().stream()
                .anyMatch(menu -> Long.valueOf(260402L).equals(menu.getMenuId())));
    }

    @Test
    @DisplayName("addMenu should return false when menu is null")
    void addMenu_nullMenu_returnsFalse() {
        boolean result = menuService.addMenu(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("addMenu should return false when menuName is blank")
    void addMenu_blankMenuName_returnsFalse() {
        Menu menu = createMenu(1L, "", "test:menu");

        boolean result = menuService.addMenu(menu);

        assertFalse(result);
    }

    @Test
    @DisplayName("addMenu should return false when menuCode is blank")
    void addMenu_blankMenuCode_returnsFalse() {
        Menu menu = createMenu(1L, "Test Menu", "");

        boolean result = menuService.addMenu(menu);

        assertFalse(result);
    }

    @Test
    @DisplayName("addMenu should return true when menu is valid")
    void addMenu_validMenu_returnsTrue() {
        Menu menu = createMenu(1L, "Test Menu", "test:menu");
        menu.setMenuType(2);
        menu.setPageType("IFRAME");
        menu.setExternalUrl("https://example.com/frame");
        when(menuMapper.insert(menu)).thenReturn(1);
        when(frontendMenuRuntimeConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(frontendMenuRuntimeConfigMapper.insert(any(FrontendMenuRuntimeConfig.class))).thenReturn(1);

        boolean result = menuService.addMenu(menu);

        assertTrue(result);
        verify(frontendMenuRuntimeConfigMapper).insert(argThat((FrontendMenuRuntimeConfig config) ->
                "IFRAME".equals(config.getPageType())
                        && "https://example.com/frame".equals(config.getExternalUrl())));
    }

    @Test
    @DisplayName("updateMenu should return false when menuId is null")
    void updateMenu_nullMenuId_returnsFalse() {
        boolean result = menuService.updateMenu(null, new Menu());

        assertFalse(result);
    }

    @Test
    @DisplayName("updateMenu should return false when menu is null")
    void updateMenu_nullMenu_returnsFalse() {
        boolean result = menuService.updateMenu(1L, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("updateMenu should return false when menu not found")
    void updateMenu_menuNotFound_returnsFalse() {
        when(menuMapper.selectById(999L)).thenReturn(null);

        boolean result = menuService.updateMenu(999L, new Menu());

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteMenu should return false when menuId is null")
    void deleteMenu_nullMenuId_returnsFalse() {
        boolean result = menuService.deleteMenu(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteMenu should return false when menu has children")
    void deleteMenu_menuHasChildren_returnsFalse() {
        Menu childMenu = createMenu(2L, "Child Menu", "child:menu");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(childMenu));

        boolean result = menuService.deleteMenu(1L);

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteMenu should return true when menu is deleted")
    void deleteMenu_menuDeleted_returnsTrue() {
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(menuMapper.deleteById(1L)).thenReturn(1);

        boolean result = menuService.deleteMenu(1L);

        assertTrue(result);
    }

    @Test
    @DisplayName("MenuServiceImpl implements IMenuService")
    void implementsIMenuService() {
        assertTrue(menuService instanceof IMenuService);
    }

    private Menu createMenu(Long menuId, String menuName, String menuCode) {
        Menu menu = new Menu();
        menu.setMenuId(menuId);
        menu.setMenuName(menuName);
        menu.setMenuCode(menuCode);
        menu.setMenuType(1);
        menu.setParentId(0L);
        menu.setStatus(1);
        menu.setVisible(1);
        menu.setSort(1);
        return menu;
    }
}
