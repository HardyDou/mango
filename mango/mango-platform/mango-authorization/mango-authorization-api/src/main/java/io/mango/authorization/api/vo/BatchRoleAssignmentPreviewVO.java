package io.mango.authorization.api.vo;

import io.mango.authorization.api.enums.BatchRoleTargetScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 批量角色绑定预览结果。 */
@Data
@Schema(description = "批量角色绑定预览结果")
public class BatchRoleAssignmentPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "目标范围")
    private BatchRoleTargetScope targetScope;

    @Schema(description = "目标成员数量")
    private Integer targetCount;

    @Schema(description = "目标成员清单")
    private List<BatchRoleMemberVO> members;
}
