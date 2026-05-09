package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.system.api.tenant.TenantDependencyChecker;
import io.mango.system.api.tenant.TenantProvisionContext;
import io.mango.system.api.tenant.TenantProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * 授权模块租户初始化。
 */
@Component
@Order(200)
@RequiredArgsConstructor
public class AuthorizationTenantProvisioner implements TenantProvisioner, TenantDependencyChecker {

    public static final String DEFAULT_APP_CODE = "internal-admin";
    public static final String DEFAULT_REALM = "INTERNAL";
    public static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";
    public static final String TENANT_ADMIN_ROLE = "ROLE_ADMIN";
    private static final Set<Long> TENANT_ADMIN_DEFAULT_MENU_IDS = Set.of(
            1L, 2L, 3L, 8L, 9L, 10L, 15L, 17L, 18L, 20L,
            1500L, 1501L, 1502L, 1503L, 1504L,
            1700L, 1701L, 1702L, 1703L, 1704L,
            2000L, 2001L, 2002L, 2003L, 2004L, 2005L, 2006L, 2007L,
            3000L, 3001L, 3002L, 3003L, 3004L, 3005L,
            9002L, 9003L, 9004L, 10002L, 10003L, 10004L
    );

    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    @Override
    public void provision(TenantProvisionContext context) {
        Role role = ensureAdminRole(context);
        grantTenantAdminDefaultMenus(context.tenantId(), role.getRoleId());
    }

    @Override
    public Optional<String> check(Long tenantId) {
        Long roleCount = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId));
        if (roleCount != null && roleCount > 0) {
            return Optional.of("机构已有关联角色/权限数据，不能直接删除");
        }
        Long roleMenuCount = roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getTenantId, tenantId));
        if (roleMenuCount != null && roleMenuCount > 0) {
            return Optional.of("机构已有关联角色菜单数据，不能直接删除");
        }
        return Optional.empty();
    }

    private Role ensureAdminRole(TenantProvisionContext context) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, context.tenantId())
                .eq(Role::getAppCode, DEFAULT_APP_CODE)
                .eq(Role::getRoleCode, TENANT_ADMIN_ROLE)
                .last("LIMIT 1"));
        if (role != null) {
            return role;
        }
        role = new Role();
        role.setTenantId(context.tenantId());
        role.setAppCode(DEFAULT_APP_CODE);
        role.setRealm(DEFAULT_REALM);
        role.setActorType(DEFAULT_ACTOR_TYPE);
        role.setRoleCode(TENANT_ADMIN_ROLE);
        role.setRoleName("机构管理员");
        role.setRoleType(1);
        role.setStatus(1);
        role.setSort(1);
        role.setRemark(context.tenantName() + " 默认机构管理员角色");
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    private void grantTenantAdminDefaultMenus(Long tenantId, Long roleId) {
        menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                        .eq(Menu::getAppCode, DEFAULT_APP_CODE)
                        .eq(Menu::getStatus, 1)
                        .eq(Menu::getDelFlag, 0)
                        .in(Menu::getMenuId, TENANT_ADMIN_DEFAULT_MENU_IDS))
                .forEach(menu -> ensureRoleMenu(tenantId, roleId, menu.getMenuId()));
    }

    private void ensureRoleMenu(Long tenantId, Long roleId, Long menuId) {
        Long count = roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getTenantId, tenantId)
                .eq(RoleMenu::getRoleId, roleId)
                .eq(RoleMenu::getMenuId, menuId));
        if (count != null && count > 0) {
            return;
        }
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setTenantId(tenantId);
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        roleMenuMapper.insert(roleMenu);
    }
}
