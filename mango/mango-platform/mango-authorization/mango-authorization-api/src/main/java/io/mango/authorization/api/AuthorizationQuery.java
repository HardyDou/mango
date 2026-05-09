package io.mango.authorization.api;


/**
 * 统一授权查询。
 */
public record AuthorizationQuery(
        Long subjectId,
        String subjectType,
        String tenantId,
        String systemCode,
        String realm,
        String actorType,
        String partyType,
        Long partyId) {

    public static final String SUBJECT_TYPE_USER = "user";
    public static final String SUBJECT_TYPE_TENANT_MEMBER = "TENANT_MEMBER";

    public AuthorizationQuery(Long subjectId, String subjectType, String tenantId, String systemCode) {
        this(subjectId, subjectType, tenantId, systemCode, null, null, null, null);
    }

    public AuthorizationQuery {
        if (subjectId == null) {
            throw new IllegalArgumentException("subjectId must not be null");
        }
        if (subjectType == null || subjectType.isBlank()) {
            throw new IllegalArgumentException("subjectType must not be blank");
        }
        subjectType = subjectType.trim();
        tenantId = tenantId == null || tenantId.isBlank() ? null : tenantId.trim();
        systemCode = systemCode == null || systemCode.isBlank() ? null : systemCode.trim();
        realm = realm == null || realm.isBlank() ? null : realm.trim();
        actorType = actorType == null || actorType.isBlank() ? null : actorType.trim();
        partyType = partyType == null || partyType.isBlank() ? null : partyType.trim();
    }

    public static AuthorizationQuery user(Long userId) {
        return new AuthorizationQuery(userId, SUBJECT_TYPE_USER, null, null);
    }

    public static AuthorizationQuery member(Long memberId) {
        return new AuthorizationQuery(memberId, SUBJECT_TYPE_TENANT_MEMBER, null, null);
    }

    public AuthorizationQuery withTenantId(String newTenantId) {
        return new AuthorizationQuery(subjectId, subjectType, newTenantId, systemCode, realm, actorType, partyType, partyId);
    }

    public AuthorizationQuery withSystemCode(String newSystemCode) {
        return new AuthorizationQuery(subjectId, subjectType, tenantId, newSystemCode, realm, actorType, partyType, partyId);
    }

    public AuthorizationQuery withRealm(String newRealm) {
        return new AuthorizationQuery(subjectId, subjectType, tenantId, systemCode, newRealm, actorType, partyType, partyId);
    }

    public AuthorizationQuery withActorType(String newActorType) {
        return new AuthorizationQuery(subjectId, subjectType, tenantId, systemCode, realm, newActorType, partyType, partyId);
    }

    public AuthorizationQuery withParty(String newPartyType, Long newPartyId) {
        return new AuthorizationQuery(subjectId, subjectType, tenantId, systemCode, realm, actorType, newPartyType, newPartyId);
    }
}
