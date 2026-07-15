package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleMenuEntity;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
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
        MangoContextSnapshot original = MangoContextHolder.get();
        MangoContextHolder.set(original.withTenantId(String.valueOf(tenantId)));
        try {
            bindPackageInTenantContext(tenantId, packageId);
        } finally {
            MangoContextHolder.set(original);
        }
    }

    private void bindPackageInTenantContext(Long tenantId, Long packageId) {
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, tenantId)
                .eq(RoleEntity::getAppCode, AuthorizationTenantProvisioner.DEFAULT_APP_CODE)
                .eq(RoleEntity::getRoleCode, AuthorizationTenantProvisioner.TENANT_ADMIN_ROLE)
                .last("LIMIT 1"));
        if (role == null) {
            return;
        }

        Set<Long> menuIds = expandMenuIds(menuPackageService.listMenuIds(packageId));
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuEntity>()
                .eq(RoleMenuEntity::getTenantId, tenantId)
                .eq(RoleMenuEntity::getRoleId, role.getRoleId()));
        menuIds.forEach(menuId -> {
            RoleMenuEntity roleMenu = new RoleMenuEntity();
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
        List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
                .eq(MenuEntity::getAppCode, AuthorizationTenantProvisioner.DEFAULT_APP_CODE)
                .eq(MenuEntity::getStatus, 1)
                .eq(MenuEntity::getDelFlag, 0));
        Map<Long, MenuEntity> menuById = menus.stream().collect(Collectors.toMap(MenuEntity::getMenuId, menu -> menu));
        Set<Long> expanded = new LinkedHashSet<>();
        for (Long menuId : selectedMenuIds) {
            Long currentId = menuId;
            while (currentId != null && currentId > 0 && expanded.add(currentId)) {
                MenuEntity menu = menuById.get(currentId);
                if (menu == null) {
                    break;
                }
                currentId = menu.getParentId();
            }
        }
        return expanded;
    }
}
