package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.core.entity.FrontendMenuRuntimeConfig;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.mapper.FrontendMenuRuntimeConfigMapper;
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
    private ISubjectAuthorityService subjectAuthorityService;

    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl(
                menuMapper,
                subjectRoleBindingMapper,
                roleMenuMapper,
                frontendMenuRuntimeConfigMapper,
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
        menu.setSort(1);
        return menu;
    }
}
