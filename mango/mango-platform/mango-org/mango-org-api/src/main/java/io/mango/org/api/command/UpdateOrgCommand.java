package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "修改组织命令")
public class UpdateOrgCommand {

    @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织ID不能为空")
    private Long id;

    @Schema(description = "父级组织ID，根节点为 0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父级组织ID不能为空")
    private Long pid;

    @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "组织名称不能为空")
    private String orgName;

    @Schema(description = "组织编码，租户内唯一", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "组织编码不能为空")
    private String orgCode;

    @Schema(description = "组织类型：1-集团，2-公司，3-部门，4-小组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织类型不能为空")
    private Integer orgType;

    @Schema(description = "排序值")
    private Integer orgSort;

    @Schema(description = "组织状态：0-禁用，1-启用")
    private String orgStatus;
}
