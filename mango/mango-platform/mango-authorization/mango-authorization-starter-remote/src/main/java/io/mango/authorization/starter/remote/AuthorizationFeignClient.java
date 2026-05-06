package io.mango.authorization.starter.remote;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.query.LoadUserAuthorizationQuery;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 授权远程客户端。
 */
@FeignClient(name = "mango-authorization", path = "/authorization")
public interface AuthorizationFeignClient extends AuthorizationApi {

    @Override
    @GetMapping("/subjects/user")
    R<AuthorizationSnapshot> loadUserAuthorization(@SpringQueryMap LoadUserAuthorizationQuery query);
}
