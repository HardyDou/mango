package io.mango.rbac.api;

import io.mango.rbac.api.po.SysUser;
import io.mango.rbac.api.vo.UserInfoVO;

/**
 * System user remote API
 * Exposed via Controller (local) or Feign Client (remote)
 *
 * @author Mango
 */
public interface SysUserApi {

    /**
     * Get current user info with permissions
     *
     * @param username username
     * @return user info with permissions
     */
    UserInfoVO getUserInfo(String username);

    /**
     * Get user info by user ID
     *
     * @param userId user ID
     * @return user info with permissions
     */
    UserInfoVO getUserInfoById(Long userId);

    /**
     * Get user entity by username (password set to null for security)
     *
     * @param username username
     * @return user entity without password
     */
    SysUser getByUsername(String username);

    /**
     * Get user entity by user ID (password set to null for security)
     *
     * @param userId user ID
     * @return user entity without password
     */
    SysUser getById(Long userId);

    /**
     * Get user entity by username with password for authentication purposes.
     * This method should ONLY be used by the auth module.
     *
     * @param username username
     * @return user entity with password hash
     */
    SysUser getByUsernameForAuth(String username);

    /**
     * Get user entity by user ID with password for authentication purposes.
     * This method should ONLY be used by the auth module.
     *
     * @param userId user ID
     * @return user entity with password hash
     */
    SysUser getByIdForAuth(Long userId);
}
