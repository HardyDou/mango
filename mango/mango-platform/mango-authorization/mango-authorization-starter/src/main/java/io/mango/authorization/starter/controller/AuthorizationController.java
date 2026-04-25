package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authorization query endpoints for remote adapters.
 */
@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
public class AuthorizationController implements AuthorizationApi {

    private final IAuthorizationProvider authorizationProvider;

    @Override
    @GetMapping("/subjects/user/{subjectId}")
    public AuthorizationSnapshot loadUserAuthorization(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String systemCode) {
        AuthorizationQuery query = AuthorizationQuery.user(subjectId)
                .withTenantId(tenantId)
                .withSystemCode(systemCode);
        return authorizationProvider.load(query);
    }
}
