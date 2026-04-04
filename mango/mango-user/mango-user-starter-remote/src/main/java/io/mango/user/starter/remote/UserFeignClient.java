package io.mango.user.starter.remote;

import io.mango.user.api.UserApi;
import io.mango.user.api.po.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * User Feign client - implements UserApi for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "user-service", path = "/user/profile")
public interface UserFeignClient extends UserApi {

    @Override
    @GetMapping("/{userId}")
    User getById(@PathVariable Long userId);

    @Override
    @PostMapping
    void create(@RequestBody User user);

    @Override
    @PutMapping
    void update(@RequestBody User user);

    @Override
    @DeleteMapping("/{userId}")
    void delete(@PathVariable Long userId);
}
