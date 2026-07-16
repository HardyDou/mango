package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改组织命令")
public class UpdateSysOrgCommand {

    @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织ID不能为空")
    @Positive(message = "组织ID必须大于0")
    private Long id;

    @Schema(description = "父级组织ID，根节点为 0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父级组织ID不能为空")
    @PositiveOrZero(message = "父级组织ID不能小于0")
    private Long pid;

    @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称长度不能超过100个字符")
    private String orgName;

    @Schema(description = "组织编码，租户内唯一", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "组织编码不能为空")
    @Size(max = 50, message = "组织编码长度不能超过50个字符")
    private String orgCode;

    @Schema(description = "组织类型：1-集团，2-公司，3-部门，4-小组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "组织类型不能为空")
    @Min(value = 1, message = "组织类型不能小于1")
    @Max(value = 4, message = "组织类型不能大于4")
    private Integer orgType;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于0")
    private Integer orgSort;

    @Schema(description = "组织状态：0-禁用，1-启用")
    @Pattern(regexp = "[01]", message = "组织状态只能为0或1")
    private String orgStatus;
}
