package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 机构成员事实。
 */
@Data
@Schema(description = "机构成员事实")
public class TenantMemberInfo {

    @Schema(description = "成员ID")
    private Long memberId;

    @Schema(description = "机构ID")
    private Long tenantId;

    @Schema(description = "全局账号ID")
    private Long userId;

    @Schema(description = "成员编号")
    private String memberNo;

    @Schema(description = "成员显示名称")
    private String displayName;

    @Schema(description = "成员类型")
    private String memberType;

    @Schema(description = "成员状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "主组织ID")
    private Long primaryOrgId;

    @Schema(description = "主岗位ID")
    private Long primaryPostId;
}
