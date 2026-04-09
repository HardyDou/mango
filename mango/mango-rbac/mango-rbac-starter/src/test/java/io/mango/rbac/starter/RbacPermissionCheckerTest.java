package io.mango.rbac.starter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.auth.api.IPermissionChecker;
import io.mango.rbac.core.entity.SysMenu;
import io.mango.rbac.core.entity.SysRole;
import io.mango.rbac.core.entity.SysRoleMenu;
import io.mango.rbac.core.entity.SysUserRole;
import io.mango.rbac.core.mapper.SysMenuMapper;
import io.mango.rbac.core.mapper.SysRoleMapper;
import io.mango.rbac.core.mapper.SysRoleMenuMapper;
import io.mango.rbac.core.mapper.SysUserRoleMapper;
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
 * Unit tests for RbacPermissionChecker
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacPermissionChecker Tests")
class RbacPermissionCheckerTest {

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysRoleMenuMapper roleMenuMapper;

    @Mock
    private SysMenuMapper menuMapper;

    private RbacPermissionChecker permissionChecker;

    @BeforeEach
    void setUp() {
        permissionChecker = new RbacPermissionChecker(userRoleMapper, roleMapper, roleMenuMapper, menuMapper);
    }

    @Test
    @DisplayName("hasPermission returns false when userId is null")
    void hasPermission_nullUserId_returnsFalse() {
        assertFalse(permissionChecker.hasPermission(null, "test:perm"));
    }

    @Test
    @DisplayName("hasPermission returns false when permission is null")
    void hasPermission_nullPermission_returnsFalse() {
        assertFalse(permissionChecker.hasPermission(1L, null));
    }

    @Test
    @DisplayName("hasPermission returns false when user has no roles")
    void hasPermission_noRoles_returnsFalse() {
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        assertFalse(permissionChecker.hasPermission(1L, "test:perm"));
    }

    @Test
    @DisplayName("hasPermission returns true for super admin (wildcard permission)")
    void hasPermission_superAdmin_returnsTrue() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole superAdminRole = createSysRole(1L, "*:*", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(superAdminRole));

        assertTrue(permissionChecker.hasPermission(1L, "any:permission:here"));
    }

    @Test
    @DisplayName("hasPermission returns true when user has the specific permission")
    void hasPermission_hasSpecificPermission_returnsTrue() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole role = createSysRole(1L, "ROLE_USER", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        SysRoleMenu roleMenu = createSysRoleMenu(1L, 1L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu));

        SysMenu menu = createSysMenu(1L, "user:admin:edit");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu));

        assertTrue(permissionChecker.hasPermission(1L, "user:admin:edit"));
    }

    @Test
    @DisplayName("hasPermission returns false when user lacks the permission")
    void hasPermission_lacksPermission_returnsFalse() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole role = createSysRole(1L, "ROLE_USER", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        SysRoleMenu roleMenu = createSysRoleMenu(1L, 1L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu));

        SysMenu menu = createSysMenu(1L, "user:admin:edit");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu));

        assertFalse(permissionChecker.hasPermission(1L, "user:admin:delete"));
    }

    @Test
    @DisplayName("getUserPermissions returns empty list when userId is null")
    void getUserPermissions_nullUserId_returnsEmptyList() {
        assertTrue(permissionChecker.getUserPermissions(null).isEmpty());
    }

    @Test
    @DisplayName("getUserPermissions returns wildcard for super admin")
    void getUserPermissions_superAdmin_returnsWildcard() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole superAdminRole = createSysRole(1L, "*:*", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(superAdminRole));

        List<String> perms = permissionChecker.getUserPermissions(1L);

        assertEquals(1, perms.size());
        assertEquals("*:*", perms.get(0));
    }

    @Test
    @DisplayName("getUserPermissions parses comma-separated permissions correctly")
    void getUserPermissions_commaSeparatedPermissions_parsesCorrectly() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole role = createSysRole(1L, "ROLE_USER", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        SysRoleMenu roleMenu = createSysRoleMenu(1L, 1L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu));

        SysMenu menu = createSysMenu(1L, "user:admin:read, user:admin:edit, user:admin:delete");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu));

        List<String> perms = permissionChecker.getUserPermissions(1L);

        assertEquals(3, perms.size());
        assertTrue(perms.contains("user:admin:read"));
        assertTrue(perms.contains("user:admin:edit"));
        assertTrue(perms.contains("user:admin:delete"));
    }

    @Test
    @DisplayName("getUserPermissions handles permissions with whitespace")
    void getUserPermissions_permissionsWithWhitespace_trimsCorrectly() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole role = createSysRole(1L, "ROLE_USER", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        SysRoleMenu roleMenu = createSysRoleMenu(1L, 1L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu));

        SysMenu menu = createSysMenu(1L, "user:admin:read,  user:admin:edit  ");
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu));

        List<String> perms = permissionChecker.getUserPermissions(1L);

        assertEquals(2, perms.size());
        assertTrue(perms.contains("user:admin:read"));
        assertTrue(perms.contains("user:admin:edit"));
    }

    @Test
    @DisplayName("getUserPermissions returns empty list when menu has null permissions")
    void getUserPermissions_nullPermissions_returnsEmptyList() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole role = createSysRole(1L, "ROLE_USER", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        SysRoleMenu roleMenu = createSysRoleMenu(1L, 1L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu));

        SysMenu menu = createSysMenu(1L, null);
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu));

        List<String> perms = permissionChecker.getUserPermissions(1L);

        assertTrue(perms.isEmpty());
    }

    @Test
    @DisplayName("getUserRoles returns empty list when userId is null")
    void getUserRoles_nullUserId_returnsEmptyList() {
        assertTrue(permissionChecker.getUserRoles(null).isEmpty());
    }

    @Test
    @DisplayName("getUserRoles returns empty list when user has no roles")
    void getUserRoles_noRoles_returnsEmptyList() {
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        assertTrue(permissionChecker.getUserRoles(1L).isEmpty());
    }

    @Test
    @DisplayName("getUserRoles returns role codes filtered by status=1")
    void getUserRoles_withRoles_returnsRoleCodes() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        SysRole role = createSysRole(1L, "ROLE_ADMIN", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        List<String> roles = permissionChecker.getUserRoles(1L);

        assertEquals(1, roles.size());
        assertEquals("ROLE_ADMIN", roles.get(0));
    }

    @Test
    @DisplayName("getUserRoles excludes disabled roles (status != 1)")
    void getUserRoles_disabledRoleExcluded() {
        SysUserRole userRole = createSysUserRole(1L, 1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

        // No roles returned because the only role has status=0
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<String> roles = permissionChecker.getUserRoles(1L);

        assertTrue(roles.isEmpty());
    }

    @Test
    @DisplayName("RbacPermissionChecker implements IPermissionChecker")
    void implementsIPermissionChecker() {
        assertTrue(permissionChecker instanceof IPermissionChecker);
    }

    private SysUserRole createSysUserRole(Long userId, Long roleId) {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        return ur;
    }

    private SysRole createSysRole(Long roleId, String roleCode, Integer status) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setRoleCode(roleCode);
        role.setStatus(status);
        return role;
    }

    private SysRoleMenu createSysRoleMenu(Long roleId, Long menuId) {
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(roleId);
        rm.setMenuId(menuId);
        return rm;
    }

    private SysMenu createSysMenu(Long menuId, String permissions) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setPermissions(permissions);
        menu.setStatus(1);
        return menu;
    }
}
