package io.mango.authorization.access.core.auth;

import io.mango.authorization.access.core.AccessConstants;
import io.mango.authorization.access.core.config.DynamicWhiteListConfig;
import io.mango.authorization.access.core.config.AccessProperties;
import io.mango.infra.security.api.ITokenProvider;
import lombok.RequiredArgsConstructor;

/**
 * 网关访问决策服务。
 *
 * @author Mango
 */
@RequiredArgsConstructor
public class AccessService {

    private final AccessProperties properties;
    private final ITokenProvider tokenService;
    private final DynamicWhiteListConfig whiteListConfig;

    public AccessResult check(String path, String authHeader) {
        if (whiteListConfig.isInternalPath(path)) {
            return AccessResult.forbidden("Internal API not accessible");
        }
        if (whiteListConfig.isAnonymousPath(path) || isStaticWhiteList(path)) {
            return AccessResult.allowAnonymous();
        }
        if (!properties.isAuthEnabled()) {
            return AccessResult.disabled();
        }
        if (authHeader == null || !authHeader.startsWith(ITokenProvider.BEARER_PREFIX)) {
            return AccessResult.unauthorized("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(ITokenProvider.BEARER_PREFIX.length());
        if (!tokenService.validateToken(token)) {
            return AccessResult.unauthorized("Invalid or expired token");
        }
        String tokenType = tokenService.getTokenType(token);
        if (!ITokenProvider.TOKEN_TYPE_ACCESS.equals(tokenType)) {
            return AccessResult.unauthorized("Invalid token type: bearer token must be access token");
        }
        return AccessResult.allowAuthenticated(resolvePrincipal(token));
    }

    private AccessPrincipal resolvePrincipal(String token) {
        return new AccessPrincipal(
                tokenService.getUserId(token),
                tokenService.getUsername(token),
                tokenService.getClaim(token, "tenantId"),
                tokenService.getClaim(token, "realm"),
                tokenService.getClaim(token, "actorType"),
                tokenService.getClaim(token, "partyType"),
                parseLong(tokenService.getClaim(token, "partyId")),
                tokenService.getClaim(token, "appCode")
        );
    }

    private boolean isStaticWhiteList(String path) {
        for (String pattern : AccessConstants.DEFAULT_ANONYMOUS_PATHS) {
            if (DynamicWhiteListConfig.matchPattern(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
