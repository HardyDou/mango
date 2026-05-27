package io.mango.identity.starter.remote;

import io.mango.common.result.R;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.IdentityUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 身份用户资料 Feign 客户端。
 */
@FeignClient(name = "mango-identity", path = "/identity")
public interface IdentityUserFeignClient extends IdentityUserApi {

    @Override
    @GetMapping("/user/info/username")
    R<IdentityUserInfo> getUserInfo(@RequestParam("username") String username);

    @Override
    @GetMapping("/user/info/id")
    R<IdentityUserInfo> getUserInfoById(@RequestParam("userId") Long userId);

    @Override
    @GetMapping("/user/info/targets")
    R<List<IdentityUserInfo>> listUserInfosByTarget(@SpringQueryMap IdentityUserTargetQuery query);
}
