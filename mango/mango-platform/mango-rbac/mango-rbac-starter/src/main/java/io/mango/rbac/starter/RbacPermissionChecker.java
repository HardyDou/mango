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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC permission checker implementation.
 * Implements {@link IPermissionChecker} defined in mango-auth-api.
 * <p>
 * This is an injection-type interface: Auth domain defines it, RBAC domain implements it.
 * Auth domain does NOT know about RBAC data models (menu, role) - only the permission checking capability.
 *
 * @author Mango
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RbacPermissionChecker implements IPermissionChecker {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    /**
     * Special permission code for super admin (has all permissions)
     */
    private static final String SUPER_ADMIN_PERMISSION = "*:*";

    @Override
    public boolean hasPermission(Long userId, String permission) {
        if (userId == null || permission == null) {
            return false;
        }

        // Get user's roles
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return false;
        }

        // Check if user has super admin role (hasPermission all permissions)
        if (hasSuperAdminRole(roleIds)) {
            return true;
        }

        // Get all permission codes from user's roles' menus
        Set<String> userPermissions = getUserPermissionsFromRoles(roleIds);

        return userPermissions.contains(permission);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return List.of();
        }

        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        // Super admin has all permissions
        if (hasSuperAdminRole(roleIds)) {
            // Return wildcard to indicate all permissions
            return List.of(SUPER_ADMIN_PERMISSION);
        }

        Set<String> permissions = getUserPermissionsFromRoles(roleIds);
        return new ArrayList<>(permissions);
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        if (userId == null) {
            return List.of();
        }

        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRole::getRoleId, roleIds)
               .eq(SysRole::getStatus, 1);
        List<SysRole> roles = roleMapper.selectList(wrapper);

        return roles.stream()
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get user's role IDs
     */
    private List<Long> getUserRoleIds(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = userRoleMapper.selectList(wrapper);
        return userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has super admin role
     */
    private boolean hasSuperAdminRole(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRole::getRoleId, roleIds)
               .eq(SysRole::getStatus, 1);
        List<SysRole> roles = roleMapper.selectList(wrapper);
        return roles.stream()
                .anyMatch(role -> SUPER_ADMIN_PERMISSION.equals(role.getRoleCode()));
    }

    /**
     * Get all permission codes from user's roles' menus.
     * Uses efficient JOIN query to avoid N+1 problem.
     */
    private Set<String> getUserPermissionsFromRoles(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }

        // Step 1: Get menu IDs for all user's roles
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(SysRoleMenu::getRoleId, roleIds);
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(roleMenuWrapper);

        if (roleMenus.isEmpty()) {
            return new HashSet<>();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // Step 2: Get permissions from menus
        LambdaQueryWrapper<SysMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(SysMenu::getMenuId, menuIds)
                   .eq(SysMenu::getStatus, 1)
                   .isNotNull(SysMenu::getPermissions)
                   .ne(SysMenu::getPermissions, "");
        List<SysMenu> menus = menuMapper.selectList(menuWrapper);

        // Step 3: Parse permission codes from each menu
        Set<String> permissions = new HashSet<>();
        for (SysMenu menu : menus) {
            String perms = menu.getPermissions();
            if (perms != null && !perms.isBlank()) {
                // Permissions are comma-separated
                String[] permArray = perms.split(",");
                for (String perm : permArray) {
                    String trimmed = perm.trim();
                    if (!trimmed.isEmpty()) {
                        permissions.add(trimmed);
                    }
                }
            }
        }

        return permissions;
    }
}
