package io.mango.access.api.auth;

/**
 * 边界入口解析出的登录主体。
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
