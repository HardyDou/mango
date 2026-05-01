package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.common.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向远程适配器的授权查询端点。
 */
@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
public class AuthorizationController implements AuthorizationApi {

    private final IAuthorizationProvider authorizationProvider;

    @Override
    @GetMapping("/subjects/user/{subjectId}")
    public R<AuthorizationSnapshot> loadUserAuthorization(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String systemCode,
            @RequestParam(required = false) String realm,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String partyType,
            @RequestParam(required = false) Long partyId) {
        AuthorizationQuery query = AuthorizationQuery.user(subjectId)
                .withTenantId(tenantId)
                .withSystemCode(systemCode)
                .withRealm(realm)
                .withActorType(actorType)
                .withParty(partyType, partyId);
        return R.ok(authorizationProvider.load(query));
    }
}
