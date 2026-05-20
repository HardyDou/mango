package io.mango.org.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 组织成员信息。
 */
@Data
@Schema(description = "组织成员信息")
public class OrgMemberVO {

    @Schema(description = "组织成员关系ID")
    private Long relationId;

    @Schema(description = "成员ID")
    private Long memberId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "成员显示名称")
    private String memberName;

    @Schema(description = "成员类型")
    private String memberType;

    @Schema(description = "成员状态")
    private Integer status;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "是否主组织")
    private Boolean primaryFlag;

    @Schema(description = "是否组织主管")
    private Boolean leaderFlag;
}
