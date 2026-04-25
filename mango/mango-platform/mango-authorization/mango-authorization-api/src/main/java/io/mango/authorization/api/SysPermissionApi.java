package io.mango.authorization.api;

import java.util.Set;

/**
 * System permission remote API
 * Exposed via Controller (local) or Feign Client (remote)
 *
 * @author Mango
 */
public interface SysPermissionApi {

    /**
     * Get all permission codes from database
     *
     * @return set of all permission codes
     */
    Set<String> getAllPermissionCodes();
}
