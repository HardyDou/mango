package io.mango.authorization.starter.controller;

import io.mango.authorization.api.AuthorizationApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.AuthorizationSnapshot;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.query.LoadUserAuthorizationQuery;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向远程适配器的授权查询端点。
 */
@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
@Tag(name = "授权查询", description = "主体角色权限授权查询接口")
public class AuthorizationController implements AuthorizationApi {

    private final IAuthorizationProvider authorizationProvider;

    @Override
    @GetMapping("/subjects/user")
    public R<AuthorizationSnapshot> loadUserAuthorization(LoadUserAuthorizationQuery query) {
        AuthorizationQuery authorizationQuery = AuthorizationQuery.user(query.getSubjectId())
                .withTenantId(query.getTenantId())
                .withSystemCode(query.getSystemCode())
                .withRealm(query.getRealm())
                .withActorType(query.getActorType())
                .withParty(query.getPartyType(), query.getPartyId());
        return R.ok(authorizationProvider.load(authorizationQuery));
    }
}
