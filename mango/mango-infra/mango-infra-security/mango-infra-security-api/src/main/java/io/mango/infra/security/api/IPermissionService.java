package io.mango.infra.security.api;

import java.util.List;

/**
 * Permission service interface for @Perm aspect.
 * <p>
 * This interface allows the @Perm aspect to be decoupled from the actual
 * permission implementation, enabling different permission services to be
 * used in different contexts.
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
