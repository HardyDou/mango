package io.mango.permission.core.service;

import io.mango.permission.api.vo.UserInfoVO;

/**
 * System user service interface
 *
 * @author Mango
 */
public interface SysUserService {

    /**
     * Get current user info with permissions
     *
     * @param username username
     * @return user info with permissions
     */
    UserInfoVO getUserInfo(String username);

    /**
     * Get current user info with permissions by user ID
     *
     * @param userId user ID
     * @return user info with permissions
     */
    UserInfoVO getUserInfoById(Long userId);
}
