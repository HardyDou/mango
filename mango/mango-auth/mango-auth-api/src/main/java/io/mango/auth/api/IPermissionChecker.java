package io.mango.auth.api;

import java.util.List;

/**
 * Permission checker interface (DIP injection type).
 * <p>
 * Defined by Auth domain, implemented by RBAC domain and injected via Spring IoC.
 * Auth domain does not know about RBAC data models (menu, role) - only the "has permission" capability.
 *
 * @author Mango
 */
public interface IPermissionChecker {

    /**
     * Check if user has a specific permission.
     *
     * @param userId      user ID
     * @param permission  permission code (e.g., "user:admin:read")
     * @return true if user has the permission, false otherwise
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * Get all permission codes for a user.
     *
     * @param userId user ID
     * @return list of permission codes (e.g., ["user:admin:read", "user:admin:edit"])
     */
    List<String> getUserPermissions(Long userId);

    /**
     * Get all role codes for a user.
     *
     * @param userId user ID
     * @return list of role codes (e.g., ["ROLE_ADMIN", "ROLE_USER"])
     */
    List<String> getUserRoles(Long userId);
}
