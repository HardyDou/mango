package io.mango.permission.core.service;

import io.mango.permission.core.entity.SysUser;
import io.mango.permission.api.vo.UserInfoVO;

import java.util.List;

/**
 * System user service interface
 *
 * @author Mango
 */
public interface ISysUserService {

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

    /**
     * Get user by username
     *
     * @param username username
     * @return user entity
     */
    SysUser getByUsername(String username);

    /**
     * Get user by user ID
     *
     * @param userId user ID
     * @return user entity
     */
    SysUser getById(Long userId);

    /**
     * List all permissions for user
     *
     * @param userId user ID
     * @return permission code list
     */
    List<String> listUserPermissions(Long userId);

    /**
     * List all roles for user
     *
     * @param userId user ID
     * @return role code list
     */
    List<String> listUserRoles(Long userId);
}
