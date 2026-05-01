package io.mango.authorization.starter.remote;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 授权远程客户端。
 */
@FeignClient(name = "mango-authorization", path = "/authorization")
public interface AuthorizationFeignClient extends AuthorizationApi {

    @Override
    @GetMapping("/subjects/user/{subjectId}")
    R<AuthorizationSnapshot> loadUserAuthorization(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String realm,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String partyType,
            @RequestParam(required = false) Long partyId);
}
