package io.mango.user.starter.controller;

import io.mango.common.annotation.Perm;
import io.mango.common.result.R;
import io.mango.user.api.UserApi;
import io.mango.user.api.po.User;
import io.mango.user.core.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User controller - implements UserApi
 *
 * @author Mango
 */
@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final IUserService userService;

    @Override
    public User getById(Long userId) {
        return userService.getById(userId);
    }

    @Override
    public void create(User user) {
        userService.create(user);
    }

    @Override
    public void update(User user) {
        userService.update(user);
    }

    @Override
    public void delete(Long userId) {
        userService.delete(userId);
    }

    /**
     * Get user profile by user ID (HTTP endpoint)
     */
    @GetMapping("/{userId}")
    @Perm(value = "user:profile:view", desc = "View user profile")
    public R<User> getByIdEndpoint(@PathVariable(value = "userId") Long userId) {
        User user = getById(userId);
        if (user == null) {
            return R.fail(404, "User profile not found");
        }
        return R.ok(user);
    }

    /**
     * Create user profile (HTTP endpoint)
     */
    @PostMapping
    @Perm(value = "user:profile:add", desc = "Create user profile")
    public R<Void> createEndpoint(@RequestBody User user) {
        create(user);
        return R.ok();
    }

    /**
     * Update user profile (HTTP endpoint)
     */
    @PutMapping
    @Perm(value = "user:profile:edit", desc = "Update user profile")
    public R<Void> updateEndpoint(@RequestBody User user) {
        update(user);
        return R.ok();
    }

    /**
     * Delete user profile (HTTP endpoint)
     */
    @DeleteMapping("/{userId}")
    @Perm(value = "user:profile:del", desc = "Delete user profile")
    public R<Void> deleteEndpoint(@PathVariable(value = "userId") Long userId) {
        delete(userId);
        return R.ok();
    }
}
