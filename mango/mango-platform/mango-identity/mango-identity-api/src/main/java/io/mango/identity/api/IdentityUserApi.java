package io.mango.identity.api;

import io.mango.identity.api.vo.IdentityUserInfo;

/**
 * Identity user remote API
 * Exposed via Controller (local) or Feign Client (remote)
 *
 * @author Mango
 */
public interface IdentityUserApi {

    /**
     * Get current user info with permissions
     *
     * @param username username
     * @return user info with permissions
     */
    IdentityUserInfo getUserInfo(String username);

    /**
     * Get user info by user ID
     *
     * @param userId user ID
     * @return user info with permissions
     */
    IdentityUserInfo getUserInfoById(Long userId);

}
