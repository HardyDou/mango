package io.mango.infra.persistence.api.context;

/**
 * 持久化执行上下文。
 * <p>
 * 表达当前数据库写入链路可使用的主体、租户、应用等运行时事实。
 */
public record PersistenceContext(
        Long userId,
        String principalName,
        String tenantId,
        String realm,
        String actorType,
        String partyType,
        Long partyId,
        Long orgId,
        String appCode
) {

    public static PersistenceContext empty() {
        return new PersistenceContext(null, null, null, null, null, null, null, null, null);
    }
}
