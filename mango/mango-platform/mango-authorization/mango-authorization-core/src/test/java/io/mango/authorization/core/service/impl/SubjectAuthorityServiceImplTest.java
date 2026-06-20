package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectAuthorityServiceImpl Tests")
class SubjectAuthorityServiceImplTest {

    @Mock
    private SubjectRoleBindingMapper subjectRoleBindingMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private RoleMenuMapper roleMenuMapper;

    @Mock
    private MenuMapper menuMapper;

    @Test
    @DisplayName("listSubjectPermissions should use menu permissions as granted permission codes")
    void listSubjectPermissions_menuCodeSeparatedFromPermission_returnsPermissions() {
        SubjectAuthorityServiceImpl service = service();
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding(10L)));
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu(100L), roleMenu(101L)));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                menu("workflow:start-process:definition-list", "workflow:definition:list"),
                menu("workflow:start-process", "workflow:definition:list,workflow:process:start")));

        List<String> permissions = service.listSubjectPermissions(query());

        assertEquals(List.of("workflow:definition:list", "workflow:process:start"), permissions);
    }

    @Test
    @DisplayName("listSubjectPermissions should fallback to menu code for legacy button menus")
    void listSubjectPermissions_legacyMenuWithoutPermissions_returnsMenuCode() {
        SubjectAuthorityServiceImpl service = service();
        when(subjectRoleBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding(10L)));
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(roleMenu(100L)));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                menu("system:user:view", null)));

        List<String> permissions = service.listSubjectPermissions(query());

        assertEquals(List.of("system:user:view"), permissions);
    }

    private SubjectAuthorityServiceImpl service() {
        return new SubjectAuthorityServiceImpl(subjectRoleBindingMapper, roleMapper, roleMenuMapper, menuMapper);
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

    private RoleMenu roleMenu(Long menuId) {
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private Menu menu(String menuCode, String permissions) {
        Menu menu = new Menu();
        menu.setMenuCode(menuCode);
        menu.setPermissions(permissions);
        return menu;
    }
}
