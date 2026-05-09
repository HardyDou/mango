package io.mango.authorization.api;

/**
 * 不可变安全上下文快照。
 *
 * @param userId 当前认证主体 ID
 * @param memberId 当前机构成员 ID
 * @param tenantId 当前机构标识
 * @param authenticated 当前请求是否已认证
 * @param principalName 当前认证主体名称
 * @param realm 登录域
 * @param actorType 操作者类型
 * @param partyType 归属主体类型
 * @param partyId 归属主体 ID
 * @param appCode 当前应用编码
 */
public record SecurityContext(
        Long userId,
        Long memberId,
        String tenantId,
        boolean authenticated,
        String principalName,
        String realm,
        String actorType,
        String partyType,
        Long partyId,
        String appCode) {

    public SecurityContext(Long userId, String tenantId, boolean authenticated, String principalName) {
        this(userId, null, tenantId, authenticated, principalName, null, null, null, null, null);
    }

    /**
     * 未认证或非请求执行路径使用的匿名上下文。
     *
     * @return 匿名上下文
     */
    public static SecurityContext anonymous() {
        return new SecurityContext(null, null, null, false, null, null, null, null, null, null);
    }
}
