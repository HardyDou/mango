package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 成员组织关系事实。
 */
@Data
@Schema(description = "成员组织关系事实")
public class TenantMemberOrgRelationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关系ID")
    private Long relationId;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "成员ID")
    private Long memberId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "成员显示名称")
    private String displayName;

    @Schema(description = "成员类型")
    private String memberType;

    @Schema(description = "成员状态")
    private Integer status;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "是否主组织")
    private Boolean primaryFlag;

    @Schema(description = "是否组织主管")
    private Boolean leaderFlag;
}
