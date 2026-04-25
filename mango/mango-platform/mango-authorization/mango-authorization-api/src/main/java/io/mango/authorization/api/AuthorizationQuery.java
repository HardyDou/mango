package io.mango.authorization.api;

/**
 * Unified authorization query.
 */
public record AuthorizationQuery(Long subjectId, String subjectType, String tenantId, String systemCode) {

    public static final String SUBJECT_TYPE_USER = "user";

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
    }

    public static AuthorizationQuery user(Long userId) {
        return new AuthorizationQuery(userId, SUBJECT_TYPE_USER, null, null);
    }

    public AuthorizationQuery withTenantId(String newTenantId) {
        return new AuthorizationQuery(subjectId, subjectType, newTenantId, systemCode);
    }

    public AuthorizationQuery withSystemCode(String newSystemCode) {
        return new AuthorizationQuery(subjectId, subjectType, tenantId, newSystemCode);
    }
}
