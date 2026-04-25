package io.mango.infra.security.core.impl;

import io.mango.infra.security.api.IPermissionService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default permission service implementation with in-memory storage.
 * <p>
 * This implementation is only used when no other IPermissionService bean
 * is available (e.g., when mango-authorization-starter is not on classpath).
 * Production use should rely on the authorization module's implementation.
 *
 * @author Mango
 */
@Slf4j
public class DefaultPermissionServiceImpl implements IPermissionService {

    /**
     * In-memory permission storage: userId -> list of permission codes
     */
    private final Map<Long, List<String>> permissionStore = new ConcurrentHashMap<>();

    @Override
    public List<String> listUserPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> perms = permissionStore.get(userId);
        if (perms == null) {
            log.debug("No permissions found for userId={}, returning empty list", userId);
            return Collections.emptyList();
        }
        return perms;
    }

    /**
     * Add permissions for a user (for testing or simple setups)
     */
    public void addPermissions(Long userId, List<String> permissions) {
        if (userId != null && permissions != null) {
            permissionStore.put(userId, permissions);
        }
    }

    /**
     * Clear all permissions (for testing)
     */
    public void clear() {
        permissionStore.clear();
    }
}
