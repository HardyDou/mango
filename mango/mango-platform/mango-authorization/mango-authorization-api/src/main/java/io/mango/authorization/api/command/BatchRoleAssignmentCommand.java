package io.mango.authorization.api.command;

import io.mango.authorization.api.enums.BatchRoleTargetScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 租户范围批量角色绑定命令。
 */
@Data
@Schema(description = "租户范围批量角色绑定命令")
public class BatchRoleAssignmentCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "角色编码")
    private String roleCode;

    @Size(max = 64)
    @Schema(description = "应用编码，未填写时使用当前应用")
    private String appCode;

    @Size(max = 32)
    @Schema(description = "登录域，未填写时使用当前登录域")
    private String realm;

    @Size(max = 32)
    @Schema(description = "操作者类型，未填写时使用当前操作者类型")
    private String actorType;

    @Size(max = 64)
    @Schema(description = "归属主体类型，未填写时使用当前上下文")
    private String partyType;

    @Positive
    @Schema(description = "归属主体ID，未填写时使用当前上下文")
    private Long partyId;

    @NotNull
    @Schema(description = "成员目标范围")
    private BatchRoleTargetScope targetScope;

    @Size(max = 10000)
    @Schema(description = "目标成员ID列表，仅 SUBJECT_IDS 生效，最多10000个")
    private List<@Positive Long> subjectIds;

    @Positive
    @Schema(description = "目标组织ID，仅 ORGANIZATION 或 ORGANIZATION_AND_DESCENDANTS 生效")
    private Long orgId;
}
