package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/** 不可变安全上下文快照。 */
@Getter
@EqualsAndHashCode
@ToString
@Schema(description = "安全上下文快照")
public final class SecurityContextVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前认证主体ID") private final Long userId;
    @Schema(description = "当前机构成员ID") private final Long memberId;
    @Schema(description = "当前机构标识") private final String tenantId;
    @Schema(description = "是否已经认证") private final boolean authenticated;
    @Schema(description = "当前认证主体名称") private final String principalName;
    @Schema(description = "登录域") private final String realm;
    @Schema(description = "操作者类型") private final String actorType;
    @Schema(description = "归属主体类型") private final String partyType;
    @Schema(description = "归属主体ID") private final Long partyId;
    @Schema(description = "当前应用编码") private final String appCode;

    public SecurityContextVO(Long userId, String tenantId, boolean authenticated, String principalName) {
        this(userId, null, tenantId, authenticated, principalName, null, null, null, null, null);
    }

    public SecurityContextVO(Long userId,
                             Long memberId,
                             String tenantId,
                             boolean authenticated,
                             String principalName,
                             String realm,
                             String actorType,
                             String partyType,
                             Long partyId,
                             String appCode) {
        this.userId = userId;
        this.memberId = memberId;
        this.tenantId = tenantId;
        this.authenticated = authenticated;
        this.principalName = principalName;
        this.realm = realm;
        this.actorType = actorType;
        this.partyType = partyType;
        this.partyId = partyId;
        this.appCode = appCode;
    }

    public static SecurityContextVO anonymous() {
        return new SecurityContextVO(null, null, null, false, null, null, null, null, null, null);
    }

    public Long userId() { return userId; }
    public Long memberId() { return memberId; }
    public String tenantId() { return tenantId; }
    public boolean authenticated() { return authenticated; }
    public String principalName() { return principalName; }
    public String realm() { return realm; }
    public String actorType() { return actorType; }
    public String partyType() { return partyType; }
    public Long partyId() { return partyId; }
    public String appCode() { return appCode; }
}
