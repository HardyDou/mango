package io.mango.workflow.api.command;

import jakarta.validation.constraints.NotNull;
import io.mango.workflow.api.validation.WorkflowOptionalValidation;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 推送流程模板命令。
 */
@Data
@Schema(description = "推送流程模板命令")
public class PushWorkflowTemplatesCommand {

    @Schema(description = "目标租户ID列表")
    @NotEmpty(message = "目标租户不能为空")
    private List<Long> targetTenantIds;

    @Schema(description = "目标业务域编码")
    @NotBlank(message = "业务域编码不能为空")
    @Size(max = 64, message = "业务域编码最多64个字符")
    private String domainCode;

    @Schema(description = "所属组织ID")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long orgId;

    @Schema(description = "历史模板分类ID，业务域替换后前台不再使用")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private Long templateCategoryId;

    @Schema(description = "模板ID列表。选择具体模板推送时传入")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private List<Long> templateIds;

    @Schema(description = "流程管理员用户名列表。为空时沿用模板管理员配置")
    @NotNull(groups = WorkflowOptionalValidation.class)
    private List<String> adminUsers;
}
