package io.mango.authorization.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/** 批量角色绑定目标成员摘要。 */
@Data
@Schema(description = "批量角色绑定目标成员摘要")
public class BatchRoleMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "成员ID")
    private Long subjectId;

    @Schema(description = "成员编号")
    private String memberNo;

    @Schema(description = "成员显示名称")
    private String displayName;

    @Schema(description = "主组织ID")
    private Long primaryOrgId;
}
