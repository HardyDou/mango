package io.mango.authorization.core.service.impl;

import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.core.service.IAuthorizationContextService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 当前请求授权上下文服务实现。 */
@Service
@RequiredArgsConstructor
public class AuthorizationContextService implements IAuthorizationContextService {

    private final ITokenProvider tokenProvider;

    @Override
    public AuthorizationQuery current(String appCode) {
        MangoContextSnapshot context = MangoContextHolder.get();
        String token = stripBearer(MangoContextHolder.token());
        Long memberId = context.memberId() != null
                ? context.memberId() : parseLong(tokenProvider.getClaim(token, "memberId"));
        if (memberId == null) {
            return null;
        }
        String effectiveAppCode = StringUtils.hasText(appCode)
                ? appCode : firstText(context.appCode(), tokenProvider.getClaim(token, "appCode"));
        return AuthorizationQuery.member(memberId)
                .withTenantId(firstText(context.tenantId(), tokenProvider.getClaim(token, "tenantId")))
                .withSystemCode(effectiveAppCode)
                .withRealm(firstText(context.realm(), tokenProvider.getClaim(token, "realm")))
                .withActorType(firstText(context.actorType(), tokenProvider.getClaim(token, "actorType")))
                .withParty(firstText(context.partyType(), tokenProvider.getClaim(token, "partyType")),
                        context.partyId() != null ? context.partyId() : parseLong(tokenProvider.getClaim(token, "partyId")));
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String stripBearer(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return token.startsWith(ITokenProvider.BEARER_PREFIX)
                ? token.substring(ITokenProvider.BEARER_PREFIX.length()) : token;
    }
}
