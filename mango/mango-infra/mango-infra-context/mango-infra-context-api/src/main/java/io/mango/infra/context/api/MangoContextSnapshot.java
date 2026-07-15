package io.mango.infra.context.api;

import io.mango.common.contract.LocalCapabilityContract;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Mango 运行时上下文快照。
 * <p>
 * 该对象只表达当前执行链路上的运行时事实，不负责认证、授权或业务判定。
 *
 * @author Mango
 */
@LocalCapabilityContract
public record MangoContextSnapshot(
        String requestId,
        String traceId,
        String tenantId,
        Long userId,
        Long memberId,
        String principalName,
        String realm,
        String actorType,
        String partyType,
        Long partyId,
        String appCode,
        String clientIp
) {

    public MangoContextSnapshot {
        requestId = normalize(requestId);
        traceId = normalize(traceId);
        tenantId = normalize(tenantId);
        principalName = normalize(principalName);
        realm = normalize(realm);
        actorType = normalize(actorType);
        partyType = normalize(partyType);
        appCode = normalize(appCode);
        clientIp = normalize(clientIp);
    }

    public static MangoContextSnapshot empty() {
        return new MangoContextSnapshot(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static MangoContextSnapshot request(String requestId,
                                               String traceId,
                                               String tenantId,
                                               String appCode,
                                               String clientIp) {
        return empty().withRequest(requestId, traceId, tenantId, appCode, clientIp);
    }

    public boolean isEmpty() {
        return Stream.of(requestId, traceId, tenantId, userId, memberId, principalName,
                        realm, actorType, partyType, partyId, appCode, clientIp)
                .allMatch(Objects::isNull);
    }

    public MangoContextSnapshot withRequest(String requestId,
                                            String traceId,
                                            String tenantId,
                                            String appCode,
                                            String clientIp) {
        return new MangoContextSnapshot(
                firstText(requestId, this.requestId),
                firstText(traceId, this.traceId),
                firstText(tenantId, this.tenantId),
                userId,
                memberId,
                principalName,
                realm,
                actorType,
                partyType,
                partyId,
                firstText(appCode, this.appCode),
                firstText(clientIp, this.clientIp)
        );
    }

    public MangoContextSnapshot withSecurity(Long userId,
                                             Long memberId,
                                             String tenantId,
                                             String principalName,
                                             String realm,
                                             String actorType,
                                             String partyType,
                                             Long partyId,
                                             String appCode) {
        return new MangoContextSnapshot(
                requestId,
                traceId,
                firstText(tenantId, this.tenantId),
                firstValue(userId, this.userId),
                firstValue(memberId, this.memberId),
                firstText(principalName, this.principalName),
                firstText(realm, this.realm),
                firstText(actorType, this.actorType),
                firstText(partyType, this.partyType),
                firstValue(partyId, this.partyId),
                firstText(appCode, this.appCode),
                clientIp
        );
    }

    public MangoContextSnapshot withSecurity(Long userId,
                                             String tenantId,
                                             String principalName,
                                             String realm,
                                             String actorType,
                                             String partyType,
                                             Long partyId,
                                             String appCode) {
        return withSecurity(userId, null, tenantId, principalName, realm, actorType, partyType, partyId, appCode);
    }

    public MangoContextSnapshot withTenantId(String tenantId) {
        return new MangoContextSnapshot(
                requestId,
                traceId,
                firstText(tenantId, this.tenantId),
                userId,
                memberId,
                principalName,
                realm,
                actorType,
                partyType,
                partyId,
                appCode,
                clientIp
        );
    }

    private static String firstText(String first, String second) {
        String normalized = normalize(first);
        if (normalized != null) {
            return normalized;
        }
        return normalize(second);
    }

    private static <T> T firstValue(T first, T second) {
        if (first != null) {
            return first;
        }
        return second;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
