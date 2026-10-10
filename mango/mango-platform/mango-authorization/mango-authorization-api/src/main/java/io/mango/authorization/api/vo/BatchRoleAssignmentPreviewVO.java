package io.mango.authorization.api.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.enums.BatchRoleTargetScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Response binding requires a mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Response binding requires a mutable collection setter"))
    private List<BatchRoleMemberVO> members;
}
