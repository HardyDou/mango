package io.mango.user.core.service;

import io.mango.user.api.po.User;

/**
 * User service interface
 *
 * @author Mango
 */
public interface IUserService {

    /**
     * Get user profile by user ID
     *
     * @param userId user ID
     * @return user profile
     */
    User getById(Long userId);

    /**
     * Create user profile
     *
     * @param user user profile
     * @return true if created successfully
     */
    boolean create(User user);

    /**
     * Update user profile
     *
     * @param user user profile
     * @return true if updated successfully
     */
    boolean update(User user);

    /**
     * Delete user profile
     *
     * @param userId user ID
     * @return true if deleted successfully
     */
    boolean delete(Long userId);
}
