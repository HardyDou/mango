package io.mango.identity.starter.remote;

import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Identity user Feign client - implements IdentityUserApi for remote calls
 *
 * @author Mango
 */
@FeignClient(name = "mango-identity", path = "/identity/user")
public interface IdentityUserFeignClient extends IdentityUserApi {

    @Override
    @GetMapping("/info/username/{username}")
    IdentityUserInfo getUserInfo(@PathVariable String username);

    @Override
    @GetMapping("/info/id/{userId}")
    IdentityUserInfo getUserInfoById(@PathVariable Long userId);

}
