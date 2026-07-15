package io.mango.authorization.starter.remote;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.query.LoadUserAuthorizationQuery;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 授权远程客户端。
 */
@FeignClient(name = "mango-authorization", contextId = "authorizationFeignClient", path = "/authorization")
public interface AuthorizationFeignClient extends AuthorizationApi {

    @Override
    @GetMapping("/subjects/user")
    R<AuthorizationSnapshotVO> loadUserAuthorization(@SpringQueryMap LoadUserAuthorizationQuery query);
}
