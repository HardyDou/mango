package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.system.api.tenant.TenantPackageBindingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 机构绑定套餐后，同步默认管理员角色授权。
 */
@Component
@RequiredArgsConstructor
public class TenantMenuPackageBindingHandler implements TenantPackageBindingHandler {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final IMenuPackageService menuPackageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPackage(Long tenantId, Long packageId) {
        if (tenantId == null || packageId == null) {
            return;
        }
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getAppCode, AuthorizationTenantProvisioner.DEFAULT_APP_CODE)
                .eq(Role::getRoleCode, AuthorizationTenantProvisioner.TENANT_ADMIN_ROLE)
                .last("LIMIT 1"));
        if (role == null) {
            return;
        }

        Set<Long> menuIds = expandMenuIds(menuPackageService.listMenuIds(packageId));
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getTenantId, tenantId)
                .eq(RoleMenu::getRoleId, role.getRoleId()));
        menuIds.forEach(menuId -> {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setTenantId(tenantId);
            roleMenu.setRoleId(role.getRoleId());
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        });
    }

    private Set<Long> expandMenuIds(List<Long> selectedMenuIds) {
        if (selectedMenuIds == null || selectedMenuIds.isEmpty()) {
            return Set.of();
        }
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getAppCode, AuthorizationTenantProvisioner.DEFAULT_APP_CODE)
                .eq(Menu::getStatus, 1)
                .eq(Menu::getDelFlag, 0));
        Map<Long, Menu> menuById = menus.stream().collect(Collectors.toMap(Menu::getMenuId, menu -> menu));
        Set<Long> expanded = new LinkedHashSet<>();
        for (Long menuId : selectedMenuIds) {
            Long currentId = menuId;
            while (currentId != null && currentId > 0 && expanded.add(currentId)) {
                Menu menu = menuById.get(currentId);
                if (menu == null) {
                    break;
                }
                currentId = menu.getParentId();
            }
        }
        return expanded;
    }
}
