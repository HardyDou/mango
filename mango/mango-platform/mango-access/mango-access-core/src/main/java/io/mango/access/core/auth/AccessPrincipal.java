package io.mango.access.core.auth;

/**
 * 网关解析出的登录主体。
 *
 * @author Mango
 */
public record AccessPrincipal(
        Long userId,
        Long memberId,
        String username,
        String tenantId,
        String realm,
        String actorType,
        String partyType,
        Long partyId,
        String appCode
) {
}
