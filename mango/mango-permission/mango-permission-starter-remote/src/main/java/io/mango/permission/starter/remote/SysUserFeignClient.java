package io.mango.permission.starter.remote;

import io.mango.permission.api.po.SysUser;
import io.mango.permission.api.SysUserApi;
import io.mango.permission.api.vo.UserInfoVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * System user Feign client - implements SysUserApi for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "permission-service", path = "/user")
public interface SysUserFeignClient extends SysUserApi {

    @Override
    @GetMapping("/info")
    UserInfoVO getUserInfo(@RequestParam String username);

    @Override
    @GetMapping("/info/{userId}")
    UserInfoVO getUserInfoById(@PathVariable Long userId);

    @Override
    @GetMapping("/{username}")
    SysUser getByUsername(@PathVariable String username);

    @Override
    @GetMapping("/id/{userId}")
    SysUser getById(@PathVariable Long userId);
}
