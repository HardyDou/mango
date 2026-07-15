package io.mango.authorization.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/** 统一授权查询。 */
public final class AuthorizationQuery {

    public static final String SUBJECT_TYPE_USER = "user";
    public static final String SUBJECT_TYPE_TENANT_MEMBER = "TENANT_MEMBER";
    public static final String SUBJECT_TYPE_ANONYMOUS = "ANONYMOUS";

    @NotNull @PositiveOrZero @Schema(description = "授权主体ID")
    private final Long subjectId;

    @NotBlank @Size(max = 32) @Schema(description = "授权主体类型")
    private final String subjectType;

    @Size(max = 64) @Schema(description = "租户标识")
    private final String tenantId;

    @Size(max = 64) @Schema(description = "应用或系统编码")
    private final String systemCode;

    @Size(max = 32) @Schema(description = "登录域")
    private final String realm;

    @Size(max = 32) @Schema(description = "操作者类型")
    private final String actorType;

    @Size(max = 64) @Schema(description = "归属主体类型")
    private final String partyType;

    @Positive @Schema(description = "归属主体ID")
    private final Long partyId;

    public AuthorizationQuery(Long subjectId, String subjectType, String tenantId, String systemCode) {
        this(subjectId, subjectType, tenantId, systemCode, null, null, null, null);
    }

    public AuthorizationQuery(Long subjectId,
                              String subjectType,
                              String tenantId,
                              String systemCode,
                              String realm,
                              String actorType,
                              String partyType,
                              Long partyId) {
        if (subjectId == null) {
            throw new IllegalArgumentException("subjectId must not be null");
        }
        if (subjectType == null || subjectType.isBlank()) {
            throw new IllegalArgumentException("subjectType must not be blank");
        }
        this.subjectId = subjectId;
        this.subjectType = subjectType.trim();
        this.tenantId = normalize(tenantId);
        this.systemCode = normalize(systemCode);
        this.realm = normalize(realm);
        this.actorType = normalize(actorType);
        this.partyType = normalize(partyType);
        this.partyId = partyId;
    }

    public static AuthorizationQuery user(Long userId) {
        return new AuthorizationQuery(userId, SUBJECT_TYPE_USER, null, null);
    }

    public static AuthorizationQuery member(Long memberId) {
        return new AuthorizationQuery(memberId, SUBJECT_TYPE_TENANT_MEMBER, null, null);
    }

    public static AuthorizationQuery anonymous() {
        return new AuthorizationQuery(0L, SUBJECT_TYPE_ANONYMOUS, null, null);
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

    public Long subjectId() { return subjectId; }
    public String subjectType() { return subjectType; }
    public String tenantId() { return tenantId; }
    public String systemCode() { return systemCode; }
    public String realm() { return realm; }
    public String actorType() { return actorType; }
    public String partyType() { return partyType; }
    public Long partyId() { return partyId; }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AuthorizationQuery that)) {
            return false;
        }
        return Objects.equals(subjectId, that.subjectId)
                && Objects.equals(subjectType, that.subjectType)
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(systemCode, that.systemCode)
                && Objects.equals(realm, that.realm)
                && Objects.equals(actorType, that.actorType)
                && Objects.equals(partyType, that.partyType)
                && Objects.equals(partyId, that.partyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subjectId, subjectType, tenantId, systemCode, realm, actorType, partyType, partyId);
    }

    @Override
    public String toString() {
        return "AuthorizationQuery{" +
                "subjectId=" + subjectId +
                ", subjectType='" + subjectType + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", systemCode='" + systemCode + '\'' +
                ", realm='" + realm + '\'' +
                ", actorType='" + actorType + '\'' +
                ", partyType='" + partyType + '\'' +
                ", partyId=" + partyId +
                '}';
    }
}
