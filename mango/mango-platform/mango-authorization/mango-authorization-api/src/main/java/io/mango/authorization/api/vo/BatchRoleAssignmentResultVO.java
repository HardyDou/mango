package io.mango.authorization.api.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/** 批量角色绑定执行结果。 */
@Data
@Schema(description = "批量角色绑定执行结果")
public class BatchRoleAssignmentResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "目标成员数量")
    private Integer targetCount;

    @Schema(description = "新建绑定数量")
    private Integer createdCount;

    @Schema(description = "已存在绑定数量")
    private Integer existingCount;

    @Schema(description = "解除绑定数量")
    private Integer removedCount;

    @Schema(description = "跳过的无效成员数量")
    private Integer skippedCount;

    @Schema(description = "跳过的成员ID")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Response binding requires a mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Response binding requires a mutable collection setter"))
    private List<Long> skippedSubjectIds;
}
