package io.mango.identity.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构成员账号管理信息。
 */
@Data
@Schema(description = "机构成员账号管理信息")
public class IdentityUserVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "当前机构成员ID")
    private Long memberId;

    @Schema(description = "当前机构成员名称")
    private String memberName;

    @Schema(description = "当前机构成员类型")
    private String memberType;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "登录域")
    private String realm;

    @Schema(description = "操作者类型")
    private String actorType;

    @Schema(description = "归属主体类型")
    private String partyType;

    @Schema(description = "归属主体ID")
    private Long partyId;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "账号状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "当前机构成员状态：0-禁用，1-启用")
    private Integer memberStatus;

    @Schema(description = "主组织ID")
    private Long primaryOrgId;

    @Schema(description = "当前查询组织下的成员组织关系ID")
    private Long orgRelationId;

    @Schema(description = "当前查询组织ID")
    private Long orgId;

    @Schema(description = "当前查询组织下的岗位ID")
    private Long postId;

    @Schema(description = "当前查询组织下的岗位名称")
    private String postName;

    @Schema(description = "当前查询组织下的岗位编码")
    private String postCode;

    @Schema(description = "当前查询组织下是否为主组织")
    private Boolean primaryOrgFlag;

    @Schema(description = "当前查询组织下是否为组织主管")
    private Boolean orgLeaderFlag;

    @Schema(description = "机构ID")
    private String tenantId;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
