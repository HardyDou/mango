package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.vo.RoleVO;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IRoleService;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
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
 * RoleServiceImpl 单元测试。
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl 测试")
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private SubjectRoleBindingMapper subjectRoleBindingMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(roleMapper, subjectRoleBindingMapper, roleMenuMapper);
        setTenantId("1");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("list should return all enabled roles ordered by sort")
    void list_returnsEnabledRolesOrderedBySort() {
        Role role1 = createRole(1L, "code1", "Role 1", 1);
        Role role2 = createRole(2L, "code2", "Role 2", 2);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(role1, role2));

        List<RoleVO> result = roleService.list();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getRoleId());
        assertEquals(2L, result.get(1).getRoleId());
    }

    @Test
    @DisplayName("list should return empty list when no roles")
    void list_noRoles_returnsEmptyList() {
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<RoleVO> result = roleService.list();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get should return role when exists")
    void get_existingRole_returnsRole() {
        Role role = createRole(1L, "admin", "Admin", 1);
        when(roleMapper.selectById(1L)).thenReturn(role);

        RoleVO result = roleService.get(1L);

        assertNotNull(result);
        assertEquals(1L, result.getRoleId());
        assertEquals("admin", result.getRoleCode());
    }

    @Test
    @DisplayName("get should return null when role not found")
    void get_nonExistingRole_returnsNull() {
        when(roleMapper.selectById(999L)).thenReturn(null);

        RoleVO result = roleService.get(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("create should insert role and return id")
    void create_validPo_returnsRoleId() {
        RoleCommand po = createRoleCommand(1L, "admin", "Admin", 1);
        when(roleMapper.insert(any(Role.class))).thenReturn(1);

        Long result = roleService.create(po);

        assertEquals(1L, result);
        verify(roleMapper).insert(any(Role.class));
    }

    @Test
    @DisplayName("create should set tenantId from context")
    void create_setsTenantIdFromContext() {
        setTenantId("123");
        RoleCommand po = createRoleCommand(null, "admin", "Admin", 1);
        when(roleMapper.insert(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            assertEquals(123L, role.getTenantId());
            return 1;
        });

        roleService.create(po);

        verify(roleMapper).insert(any(Role.class));
    }

    private void setTenantId(String tenantId) {
        MangoContextHolder.set(MangoContextSnapshot.request(null, null, tenantId, null, null));
    }

    @Test
    @DisplayName("update should return false when roleId is null")
    void update_nullRoleId_returnsFalse() {
        RoleCommand po = new RoleCommand();

        Boolean result = roleService.update(po);

        assertFalse(result);
        verify(roleMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("update should return false when role not found")
    void update_nonExistingRole_returnsFalse() {
        RoleCommand po = createRoleCommand(999L, "admin", "Admin", 1);
        when(roleMapper.selectById(999L)).thenReturn(null);

        Boolean result = roleService.update(po);

        assertFalse(result);
        verify(roleMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("update should return true when role is updated")
    void update_existingRole_returnsTrue() {
        Role existing = createRole(1L, "old", "Old Name", 1);
        RoleCommand po = createRoleCommand(1L, "new", "New Name", 1);
        when(roleMapper.selectById(1L)).thenReturn(existing);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        Boolean result = roleService.update(po);

        assertTrue(result);
        verify(roleMapper).updateById(any(Role.class));
    }

    @Test
    @DisplayName("delete should delete role and relationships")
    void delete_existingRole_deletesRelationshipsAndRole() {
        // 租户隔离：selectById 返回 tenantId=1 的角色，与 setUp 保持一致。
        when(roleMapper.selectById(1L)).thenReturn(createRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(subjectRoleBindingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(roleMapper.deleteById(1L)).thenReturn(1);

        Boolean result = roleService.delete(1L);

        assertTrue(result);
        verify(roleMapper).selectById(1L);
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(subjectRoleBindingMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMapper).deleteById(1L);
    }

    @Test
    @DisplayName("delete should return false when role not found")
    void delete_nonExistingRole_returnsFalse() {
        // 不存在的角色查询返回 null。
        when(roleMapper.selectById(999L)).thenReturn(null);

        Boolean result = roleService.delete(999L);

        assertFalse(result);
        verify(roleMapper).selectById(999L);
        verify(roleMapper, never()).deleteById(any());
    }

    @Test
    @DisplayName("getSubjectRoles should return empty list when subject has no roles")
    void getSubjectRoles_noRoles_returnsEmptyList() {
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<RoleVO> result = roleService.getSubjectRoles(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getSubjectRoles should return roles when subject has assignments")
    void getSubjectRoles_withRoles_returnsRoles() {
        SubjectRoleBinding subjectRole = new SubjectRoleBinding();
        subjectRole.setSubjectId(1L);
        subjectRole.setRoleId(1L);
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(subjectRole));
        Role role = createRole(1L, "admin", "Admin", 1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        List<RoleVO> result = roleService.getSubjectRoles(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getRoleId());
    }

    @Test
    @DisplayName("getRoleMenuIds should return menu ids for role")
    void getRoleMenuIds_existingRole_returnsMenuIds() {
        RoleMenu rm = new RoleMenu();
        rm.setRoleId(1L);
        rm.setMenuId(10L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rm));

        List<Long> result = roleService.getRoleMenuIds(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0));
    }

    @Test
    @DisplayName("getRoleMenuIds should return empty list when no menus")
    void getRoleMenuIds_noMenus_returnsEmptyList() {
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<Long> result = roleService.getRoleMenuIds(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("assignRoles with null roleIds should clear all roles")
    void assignRoles_nullRoleIds_clearsAllRoles() {
        when(subjectRoleBindingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = roleService.assignRoles(assignSubjectRolesCommand(1L, null));

        assertTrue(result);
        verify(subjectRoleBindingMapper).delete(any(LambdaQueryWrapper.class));
        verify(subjectRoleBindingMapper, never()).insert(any(SubjectRoleBinding.class));
    }

    @Test
    @DisplayName("assignRoles with empty roleIds should clear all roles")
    void assignRoles_emptyRoleIds_clearsAllRoles() {
        when(subjectRoleBindingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = roleService.assignRoles(assignSubjectRolesCommand(1L, Collections.emptyList()));

        assertTrue(result);
        verify(subjectRoleBindingMapper).delete(any(LambdaQueryWrapper.class));
        verify(subjectRoleBindingMapper, never()).insert(any(SubjectRoleBinding.class));
    }

    @Test
    @DisplayName("assignRoles should insert new role assignments")
    void assignRoles_withRoleIds_insertsAssignments() {
        when(subjectRoleBindingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(subjectRoleBindingMapper.insert(any(SubjectRoleBinding.class))).thenReturn(1);

        Boolean result = roleService.assignRoles(assignSubjectRolesCommand(1L, Arrays.asList(1L, 2L)));

        assertTrue(result);
        verify(subjectRoleBindingMapper, times(2)).insert(any(SubjectRoleBinding.class));
    }

    @Test
    @DisplayName("assignMenus with null menuIds should clear all menus")
    void assignMenus_nullMenuIds_clearsAllMenus() {
        // 租户隔离：角色属于当前租户。
        when(roleMapper.selectById(1L)).thenReturn(createRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = roleService.assignMenus(1L, null);

        assertTrue(result);
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMenuMapper, never()).insert(any(RoleMenu.class));
    }

    @Test
    @DisplayName("assignMenus with empty menuIds should clear all menus")
    void assignMenus_emptyMenuIds_clearsAllMenus() {
        // 租户隔离：角色属于当前租户。
        when(roleMapper.selectById(1L)).thenReturn(createRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        Boolean result = roleService.assignMenus(1L, Collections.emptyList());

        assertTrue(result);
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMenuMapper, never()).insert(any(RoleMenu.class));
    }

    @Test
    @DisplayName("assignMenus should insert new menu assignments")
    void assignMenus_withMenuIds_insertsAssignments() {
        // 租户隔离：角色属于当前租户。
        when(roleMapper.selectById(1L)).thenReturn(createRole(1L, "admin", "Admin", 1));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(roleMenuMapper.insert(any(RoleMenu.class))).thenReturn(1);

        Boolean result = roleService.assignMenus(1L, Arrays.asList(10L, 20L));

        assertTrue(result);
        verify(roleMenuMapper, times(2)).insert(any(RoleMenu.class));
    }

    @Test
    @DisplayName("RoleServiceImpl implements IRoleService")
    void implementsIRoleService() {
        assertTrue(roleService instanceof IRoleService);
    }

    private Role createRole(Long roleId, String roleCode, String roleName, Integer status) {
        Role role = new Role();
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

    private RoleCommand createRoleCommand(Long roleId, String roleCode, String roleName, Integer status) {
        RoleCommand po = new RoleCommand();
        po.setRoleId(roleId);
        po.setAppCode("internal-admin");
        po.setRealm("INTERNAL");
        po.setActorType("INTERNAL_USER");
        po.setRoleCode(roleCode);
        po.setRoleName(roleName);
        po.setRoleType(1);
        po.setStatus(status);
        po.setSort(1);
        return po;
    }

    private AssignSubjectRolesCommand assignSubjectRolesCommand(Long subjectId, List<Long> roleIds) {
        AssignSubjectRolesCommand command = new AssignSubjectRolesCommand();
        command.setSubjectId(subjectId);
        command.setAppCode("internal-admin");
        command.setRealm("INTERNAL");
        command.setActorType("INTERNAL_USER");
        command.setPartyType("INTERNAL_ORG");
        command.setPartyId(1L);
        command.setRoleIds(roleIds);
        return command;
    }
}
