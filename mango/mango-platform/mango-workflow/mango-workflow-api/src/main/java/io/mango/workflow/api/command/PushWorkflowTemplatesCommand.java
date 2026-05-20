package io.mango.workflow.api.command;

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

    @Schema(description = "目标流程分类编码。目标租户不存在该分类时自动创建")
    @NotBlank(message = "目标流程分类编码不能为空")
    @Size(max = 64, message = "目标流程分类编码最多64个字符")
    private String categoryCode;

    @Schema(description = "目标流程分类名称。目标租户不存在该分类时自动创建")
    @NotBlank(message = "目标流程分类名称不能为空")
    @Size(max = 64, message = "目标流程分类名称最多64个字符")
    private String categoryName;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "模板分类ID。按分类推送时传入")
    private Long templateCategoryId;

    @Schema(description = "模板ID列表。选择具体模板推送时传入")
    private List<Long> templateIds;

    @Schema(description = "流程管理员用户名列表。为空时沿用模板管理员配置")
    private List<String> adminUsers;
}
