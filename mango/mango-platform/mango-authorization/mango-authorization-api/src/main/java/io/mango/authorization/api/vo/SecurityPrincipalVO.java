package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/** Spring Security Authentication 中的认证主体载荷。 */
@Getter
@EqualsAndHashCode
@ToString
@Schema(description = "认证主体载荷")
public final class SecurityPrincipalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前认证主体ID") private final Long userId;
    @Schema(description = "当前机构成员ID") private final Long memberId;
    @Schema(description = "当前机构标识") private final String tenantId;
    @Schema(description = "当前认证主体名称") private final String principalName;
    @Schema(description = "登录域") private final String realm;
    @Schema(description = "操作者类型") private final String actorType;
    @Schema(description = "归属主体类型") private final String partyType;
    @Schema(description = "归属主体ID") private final Long partyId;
    @Schema(description = "当前应用编码") private final String appCode;

    public SecurityPrincipalVO(Long userId, String tenantId, String principalName) {
        this(userId, null, tenantId, principalName, null, null, null, null, null);
    }

    public SecurityPrincipalVO(Long userId,
                               Long memberId,
                               String tenantId,
                               String principalName,
                               String realm,
                               String actorType,
                               String partyType,
                               Long partyId,
                               String appCode) {
        this.userId = userId;
        this.memberId = memberId;
        this.tenantId = tenantId;
        this.principalName = principalName;
        this.realm = realm;
        this.actorType = actorType;
        this.partyType = partyType;
        this.partyId = partyId;
        this.appCode = appCode;
    }

    public Long userId() { return userId; }
    public Long memberId() { return memberId; }
    public String tenantId() { return tenantId; }
    public String principalName() { return principalName; }
    public String realm() { return realm; }
    public String actorType() { return actorType; }
    public String partyType() { return partyType; }
    public Long partyId() { return partyId; }
    public String appCode() { return appCode; }
}
