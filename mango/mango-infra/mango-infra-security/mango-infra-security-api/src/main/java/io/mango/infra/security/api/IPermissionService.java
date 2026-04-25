package io.mango.infra.security.api;

import java.util.List;

/**
 * Permission lookup interface used by Spring Security backed {@link Perm} authorization.
 *
 * @author Mango
 */
public interface IPermissionService {

    /**
     * List all permissions for a user
     *
     * @param userId user ID
     * @return list of permission codes
     */
    List<String> listUserPermissions(Long userId);
}
