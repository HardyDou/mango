package io.mango.user.starter.remote;

import io.mango.common.result.R;
import io.mango.user.api.po.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * User Feign client for remote user operations
 *
 * @author Mango
 */
@FeignClient(name = "user-service", path = "/user/profile")
public interface UserFeignClient {

    /**
     * Get user by ID
     *
     * @param userId user ID
     * @return user profile
     */
    @GetMapping("/{userId}")
    R<User> getById(@PathVariable Long userId);

    /**
     * Create user profile
     *
     * @param user user profile
     * @return void
     */
    @PostMapping
    R<Void> create(@RequestBody User user);

    /**
     * Update user profile
     *
     * @param user user profile
     * @return void
     */
    @PutMapping
    R<Void> update(@RequestBody User user);

    /**
     * Delete user profile
     *
     * @param userId user ID
     * @return void
     */
    @DeleteMapping("/{userId}")
    R<Void> delete(@PathVariable Long userId);
}
