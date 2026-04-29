package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 身份用户资料 Feign 客户端。
 */
@FeignClient(name = "mango-identity", path = "/identity")
public interface IdentityUserFeignClient extends IdentityUserApi {

    @Override
    @GetMapping("/user/info/username/{username}")
    R<IdentityUserInfo> getUserInfo(@PathVariable("username") String username);

    @Override
    @GetMapping("/user/info/id/{userId}")
    R<IdentityUserInfo> getUserInfoById(@PathVariable("userId") Long userId);
}
