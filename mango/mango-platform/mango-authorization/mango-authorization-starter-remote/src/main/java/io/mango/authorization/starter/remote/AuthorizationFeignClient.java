package io.mango.authorization.starter.remote;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.AuthorizationSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Authorization remote client.
 */
@FeignClient(name = "mango-authorization", path = "/authorization")
public interface AuthorizationFeignClient extends AuthorizationApi {

    @Override
    @GetMapping("/subjects/user/{subjectId}")
    AuthorizationSnapshot loadUserAuthorization(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String systemCode);
}
