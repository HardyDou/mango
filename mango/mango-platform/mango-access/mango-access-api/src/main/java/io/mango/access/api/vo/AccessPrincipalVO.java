package io.mango.access.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 边界入口解析出的登录主体值对象。 */
@Schema(description = "边界入口登录主体")
public final class AccessPrincipalVO {

    @Schema(description = "用户ID") private final Long userId;
    @Schema(description = "成员ID") private final Long memberId;
    @Schema(description = "用户名") private final String username;
    @Schema(description = "租户ID") private final String tenantId;
    @Schema(description = "登录域") private final String realm;
    @Schema(description = "操作者类型") private final String actorType;
    @Schema(description = "归属主体类型") private final String partyType;
    @Schema(description = "归属主体ID") private final Long partyId;
    @Schema(description = "应用编码") private final String appCode;

    public AccessPrincipalVO(Long userId, Long memberId, String username, String tenantId,
                             String realm, String actorType, String partyType, Long partyId,
                             String appCode) {
        this.userId = userId;
        this.memberId = memberId;
        this.username = username;
        this.tenantId = tenantId;
        this.realm = realm;
        this.actorType = actorType;
        this.partyType = partyType;
        this.partyId = partyId;
        this.appCode = appCode;
    }

    public Long userId() { return userId; }
    public Long memberId() { return memberId; }
    public String username() { return username; }
    public String tenantId() { return tenantId; }
    public String realm() { return realm; }
    public String actorType() { return actorType; }
    public String partyType() { return partyType; }
    public Long partyId() { return partyId; }
    public String appCode() { return appCode; }
}
