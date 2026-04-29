package io.mango.authorization.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一授权查询。
 */
@Schema(description = "统一授权查询")
public record AuthorizationQuery(
        @Schema(description = "主体ID") Long subjectId,
        @Schema(description = "主体类型，如 user") String subjectType,
        @Schema(description = "租户ID") String tenantId,
        @Schema(description = "系统编码") String systemCode) {

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
