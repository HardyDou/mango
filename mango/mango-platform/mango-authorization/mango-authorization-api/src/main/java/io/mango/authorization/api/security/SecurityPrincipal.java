package io.mango.authorization.api.security;

/**
 * 存放在 Spring Security Authentication 中的认证主体载荷。
 *
 * @param userId 当前认证主体 ID
 * @param tenantId 当前租户标识
 * @param principalName 当前认证主体名称
 * @param realm 登录域
 * @param actorType 操作者类型
 * @param partyType 归属主体类型
 * @param partyId 归属主体 ID
 * @param appCode 当前应用编码
 */
public record SecurityPrincipal(
        Long userId,
        String tenantId,
        String principalName,
        String realm,
        String actorType,
        String partyType,
        Long partyId,
        String appCode) {

    public SecurityPrincipal(Long userId, String tenantId, String principalName) {
        this(userId, tenantId, principalName, null, null, null, null, null);
    }
}
