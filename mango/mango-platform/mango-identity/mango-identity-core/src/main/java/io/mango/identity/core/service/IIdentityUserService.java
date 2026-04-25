package io.mango.identity.core.service;

import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.api.vo.IdentityUserInfo;

/**
 * Identity user service interface
 *
 * @author Mango
 */
public interface IIdentityUserService {

    /**
     * Get identity user profile by username.
     *
     * @param username username
     * @return identity user profile
     */
    IdentityUserInfo getUserInfo(String username);

    /**
     * Get identity user profile by user ID.
     *
     * @param userId user ID
     * @return identity user profile
     */
    IdentityUserInfo getUserInfoById(Long userId);

    /**
     * Get user by username
     *
     * @param username username
     * @return user entity
     */
    IdentityUser getByUsername(String username);

    /**
     * Get user by user ID
     *
     * @param userId user ID
     * @return user entity
     */
    IdentityUser getById(Long userId);

}
