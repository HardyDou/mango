package io.mango.user.api;

import io.mango.user.api.po.User;

/**
 * User profile API interface
 * Exposed via Controller (local) or Feign Client (remote)
 *
 * @author Mango
 */
public interface UserApi {

    /**
     * Get user by ID
     *
     * @param userId user ID
     * @return user profile
     */
    User getById(Long userId);

    /**
     * Create user profile
     *
     * @param user user profile
     */
    void create(User user);

    /**
     * Update user profile
     *
     * @param user user profile
     */
    void update(User user);

    /**
     * Delete user profile
     *
     * @param userId user ID
     */
    void delete(Long userId);
}
