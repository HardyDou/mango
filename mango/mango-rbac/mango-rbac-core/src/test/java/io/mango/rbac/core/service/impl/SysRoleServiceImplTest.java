package io.mango.rbac.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.infra.context.core.TenantContextHolder;
import io.mango.rbac.api.po.SysRolePo;
import io.mango.rbac.api.vo.SysRoleVO;
import io.mango.rbac.core.entity.SysRole;
import io.mango.rbac.core.entity.SysRoleMenu;
import io.mango.rbac.core.entity.SysUserRole;
import io.mango.rbac.core.entity.SysUser;
import io.mango.rbac.core.mapper.SysRoleMapper;
import io.mango.rbac.core.mapper.SysRoleMenuMapper;
import io.mango.rbac.core.mapper.SysUserMapper;
import io.mango.rbac.core.mapper.SysUserRoleMapper;
import io.mango.rbac.core.service.ISysRoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SysRoleServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysRoleServiceImpl Tests")
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysRoleMenuMapper roleMenuMapper;

    @Mock
    private SysUserMapper userMapper;

    private SysRoleServiceImpl sysRoleService;

    @BeforeEach
    void setUp() {
        sysRoleService = new SysRoleServiceImpl(roleMapper, userRoleMapper, roleMenuMapper, userMapper);
        TenantContextHolder.setTenantId("1");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("list should return all enabled roles ordered by sort")
    void list_returnsEnabledRolesOrderedBySort() {
        SysRole role1 = createSysRole(1L, "code1", "Role 1", 1);
        SysRole role2 = createSysRole(2L, "code2", "Role 2", 2);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(role1, role2));

        List<SysRoleVO> result = sysRoleService.list();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getRoleId());
        assertEquals(2L, result.get(1).getRoleId());
    }

    @Test
    @DisplayName("list should return empty list when no roles")
    void list_noRoles_returnsEmptyList() {
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<SysRoleVO> result = sysRoleService.list();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get should return role when exists")
    void get_existingRole_returnsRole() {
        SysRole role = createSysRole(1L, "admin", "Admin", 1);
        when(roleMapper.selectById(1L)).thenReturn(role);

        SysRoleVO result = sysRoleService.get(1L);

        assertNotNull(result);
        assertEquals(1L, result.getRoleId());
        assertEquals("admin", result.getRoleCode());
    }

    @Test
    @DisplayName("get should return null when role not found")
    void get_nonExistingRole_returnsNull() {
        when(roleMapper.selectById(999L)).thenReturn(null);

        SysRoleVO result = sysRoleService.get(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("create should insert role and return id")
    void create_validPo_returnsRoleId() {
        SysRolePo po = createSysRolePo(1L, "admin", "Admin", 1);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        Long result = sysRoleService.create(po);

        assertEquals(1L, result);
        verify(roleMapper).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("create should set tenantId from context")
    void create_setsTenantIdFromContext() {
        TenantContextHolder.setTenantId("123");
        SysRolePo po = createSysRolePo(null, "admin", "Admin", 1);
        when(roleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            SysRole role = invocation.getArgument(0);
            assertEquals(123L, role.getTenantId());
            return 1;
        });

        sysRoleService.create(po);

        verify(roleMapper).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("update should return false when roleId is null")
    void update_nullRoleId_returnsFalse() {
        SysRolePo po = new SysRolePo();

        Boolean result = sysRoleService.update(po);

        assertFalse(result);
        verify(roleMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("update should return false when role not found")
    void update_nonExistingRole_returnsFalse() {
        SysRolePo po = createSysRolePo(999L, "admin", "Admin", 1);
        when(roleMapper.selectById(999L)).thenReturn(null);

        Boolean result = sysRoleService.update(po);

        assertFalse(result);
        verify(roleMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("update should return true when role is updated")
    void update_existingRole_returnsTrue() {
        SysRole existing = createSysRole(1L, "old", "Old Name", 1);
        SysRolePo po = createSysRolePo(1L, "new", "New Name", 1);
        when(roleMapper.selectById(1L)).thenReturn(existing);
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);

        Boolean result = sysRoleService.update(po);

        assertTrue(result);
        verify(roleMapper).updateById(any(SysRole.class));
    }

    @Test
    @DisplayName("delete should delete role and relationships")
    void delete_existingRole_deletesRelationshipsAndRole() {
        // Tenant isolation: selectById returns role with tenantId=1 (matches setUp)
        when(roleMapper.selectById(1L)).thenReturn(createSysRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(roleMapper.deleteById(1L)).thenReturn(1);

        Boolean result = sysRoleService.delete(1L);

        assertTrue(result);
        verify(roleMapper).selectById(1L);
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMapper).deleteById(1L);
    }

    @Test
    @DisplayName("delete should return false when role not found")
    void delete_nonExistingRole_returnsFalse() {
        // selectById returns null for non-existing role
        when(roleMapper.selectById(999L)).thenReturn(null);

        Boolean result = sysRoleService.delete(999L);

        assertFalse(result);
        verify(roleMapper).selectById(999L);
        verify(roleMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("getUserRoles should return empty list when user has no roles")
    void getUserRoles_noRoles_returnsEmptyList() {
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<SysRoleVO> result = sysRoleService.getUserRoles(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getUserRoles should return roles when user has assignments")
    void getUserRoles_withRoles_returnsRoles() {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(1L);
        userRole.setRoleId(1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));
        SysRole role = createSysRole(1L, "admin", "Admin", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        List<SysRoleVO> result = sysRoleService.getUserRoles(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getRoleId());
    }

    @Test
    @DisplayName("getRoleMenuIds should return menu ids for role")
    void getRoleMenuIds_existingRole_returnsMenuIds() {
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(1L);
        rm.setMenuId(10L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rm));

        List<Long> result = sysRoleService.getRoleMenuIds(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0));
    }

    @Test
    @DisplayName("getRoleMenuIds should return empty list when no menus")
    void getRoleMenuIds_noMenus_returnsEmptyList() {
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<Long> result = sysRoleService.getRoleMenuIds(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("assignRoles with null roleIds should clear all roles")
    void assignRoles_nullRoleIds_clearsAllRoles() {
        // Tenant isolation: user belongs to current tenant
        when(userMapper.selectById(1L)).thenReturn(createSysUser(1L));
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = sysRoleService.assignRoles(1L, null);

        assertTrue(result);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("assignRoles with empty roleIds should clear all roles")
    void assignRoles_emptyRoleIds_clearsAllRoles() {
        // Tenant isolation: user belongs to current tenant
        when(userMapper.selectById(1L)).thenReturn(createSysUser(1L));
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = sysRoleService.assignRoles(1L, Collections.emptyList());

        assertTrue(result);
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("assignRoles should insert new role assignments")
    void assignRoles_withRoleIds_insertsAssignments() {
        // Tenant isolation: user belongs to current tenant
        when(userMapper.selectById(1L)).thenReturn(createSysUser(1L));
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

        Boolean result = sysRoleService.assignRoles(1L, Arrays.asList(1L, 2L));

        assertTrue(result);
        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    @DisplayName("assignMenus with null menuIds should clear all menus")
    void assignMenus_nullMenuIds_clearsAllMenus() {
        // Tenant isolation: role belongs to current tenant
        when(roleMapper.selectById(1L)).thenReturn(createSysRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = sysRoleService.assignMenus(1L, null);

        assertTrue(result);
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMenuMapper, never()).insert(any(SysRoleMenu.class));
    }

    @Test
    @DisplayName("assignMenus with empty menuIds should clear all menus")
    void assignMenus_emptyMenuIds_clearsAllMenus() {
        // Tenant isolation: role belongs to current tenant
        when(roleMapper.selectById(1L)).thenReturn(createSysRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = sysRoleService.assignMenus(1L, Collections.emptyList());

        assertTrue(result);
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMenuMapper, never()).insert(any(SysRoleMenu.class));
    }

    @Test
    @DisplayName("assignMenus should insert new menu assignments")
    void assignMenus_withMenuIds_insertsAssignments() {
        // Tenant isolation: role belongs to current tenant
        when(roleMapper.selectById(1L)).thenReturn(createSysRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(roleMenuMapper.insert(any(SysRoleMenu.class))).thenReturn(1);

        Boolean result = sysRoleService.assignMenus(1L, Arrays.asList(10L, 20L));

        assertTrue(result);
        verify(roleMenuMapper, times(2)).insert(any(SysRoleMenu.class));
    }

    @Test
    @DisplayName("SysRoleServiceImpl implements ISysRoleService")
    void implementsISysRoleService() {
        assertTrue(sysRoleService instanceof ISysRoleService);
    }

    private SysRole createSysRole(Long roleId, String roleCode, String roleName, Integer status) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setTenantId(1L);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setRoleType(1);
        role.setStatus(status);
        role.setSort(1);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        return role;
    }

    private SysUser createSysUser(Long userId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUsername("testuser");
        user.setStatus(1);
        return user;
    }

    private SysRolePo createSysRolePo(Long roleId, String roleCode, String roleName, Integer status) {
        SysRolePo po = new SysRolePo();
        po.setRoleId(roleId);
        po.setRoleCode(roleCode);
        po.setRoleName(roleName);
        po.setRoleType(1);
        po.setStatus(status);
        po.setSort(1);
        return po;
    }
}
