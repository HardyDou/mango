package io.mango.permission.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.permission.api.vo.SysMenuVO;
import io.mango.permission.core.entity.SysMenu;
import io.mango.permission.core.mapper.SysMenuMapper;
import io.mango.permission.core.service.ISysMenuService;
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
 * Unit tests for SysMenuServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysMenuServiceImpl Tests")
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    private SysMenuServiceImpl sysMenuService;

    @BeforeEach
    void setUp() {
        sysMenuService = new SysMenuServiceImpl(sysMenuMapper);
    }

    @Test
    @DisplayName("getById should return menu when exists")
    void getById_existingMenu_returnsMenu() {
        SysMenu menu = createSysMenu(1L, "Test Menu", "test:menu");
        when(sysMenuMapper.selectById(1L)).thenReturn(menu);

        SysMenu result = sysMenuService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMenuId());
    }

    @Test
    @DisplayName("listByParentId should return menus for parent")
    void listByParentId_existingParent_returnsMenus() {
        SysMenu menu1 = createSysMenu(1L, "Menu 1", "menu:1");
        SysMenu menu2 = createSysMenu(2L, "Menu 2", "menu:2");
        when(sysMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(menu1, menu2));

        List<SysMenu> result = sysMenuService.listByParentId(0L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("buildMenuTree should return empty list for null input")
    void buildMenuTree_nullInput_returnsEmptyList() {
        List<SysMenuVO> result = sysMenuService.buildMenuTree(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("buildMenuTree should return empty list for empty input")
    void buildMenuTree_emptyInput_returnsEmptyList() {
        List<SysMenuVO> result = sysMenuService.buildMenuTree(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("addMenu should return false when menu is null")
    void addMenu_nullMenu_returnsFalse() {
        boolean result = sysMenuService.addMenu(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("addMenu should return false when menuName is blank")
    void addMenu_blankMenuName_returnsFalse() {
        SysMenu menu = createSysMenu(1L, "", "test:menu");

        boolean result = sysMenuService.addMenu(menu);

        assertFalse(result);
    }

    @Test
    @DisplayName("addMenu should return false when menuCode is blank")
    void addMenu_blankMenuCode_returnsFalse() {
        SysMenu menu = createSysMenu(1L, "Test Menu", "");

        boolean result = sysMenuService.addMenu(menu);

        assertFalse(result);
    }

    @Test
    @DisplayName("addMenu should return true when menu is valid")
    void addMenu_validMenu_returnsTrue() {
        SysMenu menu = createSysMenu(1L, "Test Menu", "test:menu");
        when(sysMenuMapper.insert(menu)).thenReturn(1);

        boolean result = sysMenuService.addMenu(menu);

        assertTrue(result);
    }

    @Test
    @DisplayName("updateMenu should return false when menuId is null")
    void updateMenu_nullMenuId_returnsFalse() {
        boolean result = sysMenuService.updateMenu(null, new SysMenu());

        assertFalse(result);
    }

    @Test
    @DisplayName("updateMenu should return false when menu is null")
    void updateMenu_nullMenu_returnsFalse() {
        boolean result = sysMenuService.updateMenu(1L, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("updateMenu should return false when menu not found")
    void updateMenu_menuNotFound_returnsFalse() {
        when(sysMenuMapper.selectById(999L)).thenReturn(null);

        boolean result = sysMenuService.updateMenu(999L, new SysMenu());

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteMenu should return false when menuId is null")
    void deleteMenu_nullMenuId_returnsFalse() {
        boolean result = sysMenuService.deleteMenu(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteMenu should return false when menu has children")
    void deleteMenu_menuHasChildren_returnsFalse() {
        SysMenu childMenu = createSysMenu(2L, "Child Menu", "child:menu");
        when(sysMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(childMenu));

        boolean result = sysMenuService.deleteMenu(1L);

        assertFalse(result);
    }

    @Test
    @DisplayName("deleteMenu should return true when menu is deleted")
    void deleteMenu_menuDeleted_returnsTrue() {
        when(sysMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(sysMenuMapper.deleteById(1L)).thenReturn(1);

        boolean result = sysMenuService.deleteMenu(1L);

        assertTrue(result);
    }

    @Test
    @DisplayName("SysMenuServiceImpl implements ISysMenuService")
    void implementsISysMenuService() {
        assertTrue(sysMenuService instanceof ISysMenuService);
    }

    private SysMenu createSysMenu(Long menuId, String menuName, String menuCode) {
        SysMenu menu = new SysMenu();
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
