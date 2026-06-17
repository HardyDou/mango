package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.SaveRoleDataScopeCommand;
import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.api.vo.EffectiveDataScopeVO;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleDataScope;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.infra.context.core.MangoContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleDataScopeServiceImpl 测试")
class RoleDataScopeServiceImplTest {

    @Mock
    private RoleDataScopeMapper roleDataScopeMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @Mock
    private SubjectRoleBindingMapper subjectRoleBindingMapper;

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("resolve should fallback to SELF when subject has no role")
    void resolve_noRoles_returnsSelf() {
        RoleDataScopeServiceImpl service = service();
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        EffectiveDataScopeVO result = service.resolve(query(), "payment:order:list");

        assertEquals(DataScopeMode.SELF, result.getScopeMode());
        assertTrue(result.getSelfIncluded());
    }

    @Test
    @DisplayName("resolve should return ALL when any role grants all")
    void resolve_anyAll_returnsAll() {
        RoleDataScopeServiceImpl service = service();
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding(10L), binding(20L)));
        when(roleDataScopeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                scope(10L, "ORG", "[\"100\"]"),
                scope(20L, "ALL", "[]")));

        EffectiveDataScopeVO result = service.resolve(query(), "payment:order:list");

        assertEquals(DataScopeMode.ALL, result.getScopeMode());
        assertFalse(result.getSelfIncluded());
    }

    @Test
    @DisplayName("resolve should merge ORG values and preserve SELF")
    void resolve_orgAndSelf_mergesValuesAndSelf() {
        RoleDataScopeServiceImpl service = service();
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding(10L), binding(20L)));
        when(roleDataScopeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                scope(10L, "ORG", "[\"100\",\"200\"]"),
                scope(20L, "SELF", "[]")));

        EffectiveDataScopeVO result = service.resolve(query(), "payment:order:list");

        assertEquals(DataScopeMode.ORG, result.getScopeMode());
        assertEquals(List.of("100", "200"), result.getScopeValues());
        assertTrue(result.getSelfIncluded());
    }

    @Test
    @DisplayName("resolve should keep self org mode for runtime primary org resolution")
    void resolve_selfOrg_keepsSelfOrgMode() {
        RoleDataScopeServiceImpl service = service();
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding(10L)));
        when(roleDataScopeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                scope(10L, "SELF_ORG", "[]")));

        EffectiveDataScopeVO result = service.resolve(query(), "payment:order:list");

        assertEquals(DataScopeMode.SELF_ORG, result.getScopeMode());
        assertTrue(result.getScopeValues().isEmpty());
    }

    @Test
    @DisplayName("resolve should keep self org children mode for runtime descendant expansion")
    void resolve_selfOrgAndChildren_keepsSelfOrgChildrenMode() {
        RoleDataScopeServiceImpl service = service();
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding(10L)));
        when(roleDataScopeMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                scope(10L, "SELF_ORG_AND_CHILDREN", "[]")));

        EffectiveDataScopeVO result = service.resolve(query(), "payment:order:list");

        assertEquals(DataScopeMode.SELF_ORG_AND_CHILDREN, result.getScopeMode());
        assertTrue(result.getIncludeChildren());
    }

    @Test
    @DisplayName("save should allow data scope only for role granted list resource")
    void save_grantedQueryResource_returnsTrue() {
        RoleDataScopeServiceImpl service = service();
        SaveRoleDataScopeCommand command = saveCommand("authorization:role:list");
        when(roleMapper.selectById(2L)).thenReturn(role());
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu(3000L)));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu("authorization:role:list")));
        when(roleDataScopeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(roleDataScopeMapper.insert(any(RoleDataScope.class))).thenReturn(1);

        Boolean result = service.save(command);

        assertTrue(result);
    }

    @Test
    @DisplayName("save should reject non-list resource even when role has permission")
    void save_nonListResource_returnsFalse() {
        RoleDataScopeServiceImpl service = service();
        SaveRoleDataScopeCommand command = saveCommand("authorization:role:query");
        when(roleMapper.selectById(2L)).thenReturn(role());

        Boolean result = service.save(command);

        assertFalse(result);
    }

    @Test
    @DisplayName("save should reject resource not granted to role")
    void save_notGrantedResource_returnsFalse() {
        RoleDataScopeServiceImpl service = service();
        SaveRoleDataScopeCommand command = saveCommand("workflow:definition:list");
        when(roleMapper.selectById(2L)).thenReturn(role());
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu(3000L)));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu("authorization:role:list")));

        Boolean result = service.save(command);

        assertFalse(result);
    }

    private RoleDataScopeServiceImpl service() {
        return new RoleDataScopeServiceImpl(
                roleDataScopeMapper,
                roleMapper,
                menuMapper,
                roleMenuMapper,
                subjectRoleBindingMapper,
                new ObjectMapper());
    }

    private AuthorizationQuery query() {
        return AuthorizationQuery.member(1001L)
                .withTenantId("1")
                .withSystemCode("internal-admin")
                .withRealm("INTERNAL")
                .withActorType("INTERNAL_USER")
                .withParty("INTERNAL_ORG", 1L);
    }

    private SubjectRoleBinding binding(Long roleId) {
        SubjectRoleBinding binding = new SubjectRoleBinding();
        binding.setRoleId(roleId);
        return binding;
    }

    private RoleDataScope scope(Long roleId, String mode, String values) {
        RoleDataScope scope = new RoleDataScope();
        scope.setTenantId(1L);
        scope.setAppCode("internal-admin");
        scope.setRoleId(roleId);
        scope.setResourceCode("payment:order:list");
        scope.setScopeMode(mode);
        scope.setScopeValues(values);
        scope.setStatus(1);
        return scope;
    }

    private SaveRoleDataScopeCommand saveCommand(String resourceCode) {
        SaveRoleDataScopeCommand command = new SaveRoleDataScopeCommand();
        command.setRoleId(2L);
        command.setResourceCode(resourceCode);
        command.setScopeMode(DataScopeMode.ALL);
        command.setScopeValues(Collections.emptyList());
        command.setStatus(1);
        return command;
    }

    private Role role() {
        Role role = new Role();
        role.setRoleId(2L);
        role.setTenantId(2L);
        role.setAppCode("internal-admin");
        return role;
    }

    private RoleMenu roleMenu(Long menuId) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setTenantId(2L);
        roleMenu.setRoleId(2L);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private Menu menu(String permissions) {
        Menu menu = new Menu();
        menu.setMenuId(3000L);
        menu.setAppCode("internal-admin");
        menu.setStatus(1);
        menu.setPermissions(permissions);
        return menu;
    }
}
