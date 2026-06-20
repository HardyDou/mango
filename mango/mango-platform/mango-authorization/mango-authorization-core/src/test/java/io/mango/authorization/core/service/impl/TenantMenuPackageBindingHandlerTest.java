package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantMenuPackageBindingHandler Tests")
class TenantMenuPackageBindingHandlerTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private IMenuPackageService menuPackageService;

    private TenantMenuPackageBindingHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TenantMenuPackageBindingHandler(roleMapper, roleMenuMapper, menuMapper, menuPackageService);
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("bindPackage should run role binding in target tenant context and restore original context")
    void bindPackage_targetTenant_switchesContextAndRestoresOriginal() {
        Role role = new Role();
        role.setTenantId(2L);
        role.setRoleId(20L);
        role.setRoleCode(AuthorizationTenantProvisioner.TENANT_ADMIN_ROLE);
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            assertEquals("2", MangoContextHolder.tenantId());
            return role;
        });
        when(menuPackageService.listMenuIds(10L)).thenAnswer(invocation -> {
            assertEquals("2", MangoContextHolder.tenantId());
            return List.of(200L);
        });
        Menu root = menu(100L, 0L);
        Menu page = menu(200L, 100L);
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, page));
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            assertEquals("2", MangoContextHolder.tenantId());
            return 1;
        });

        handler.bindPackage(2L, 10L);

        assertEquals("1", MangoContextHolder.tenantId());
        ArgumentCaptor<RoleMenu> roleMenuCaptor = ArgumentCaptor.forClass(RoleMenu.class);
        verify(roleMenuMapper, times(2)).insert(roleMenuCaptor.capture());
        RoleMenu first = roleMenuCaptor.getAllValues().get(0);
        assertEquals(2L, first.getTenantId());
        assertEquals(20L, first.getRoleId());
        assertEquals(200L, first.getMenuId());
        RoleMenu second = roleMenuCaptor.getAllValues().get(1);
        assertEquals(2L, second.getTenantId());
        assertEquals(20L, second.getRoleId());
        assertEquals(100L, second.getMenuId());
    }

    private Menu menu(Long menuId, Long parentId) {
        Menu menu = new Menu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setAppCode(AuthorizationTenantProvisioner.DEFAULT_APP_CODE);
        menu.setStatus(1);
        menu.setDelFlag(0);
        return menu;
    }
}
