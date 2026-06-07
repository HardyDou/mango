package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 批量导入流程模板命令。
 */
@Data
@Schema(description = "批量导入流程模板命令")
public class ImportWorkflowTemplatesCommand {

    @Schema(description = "历史流程分类ID，业务域替换后不再作为必填归属")
    private Long categoryId;

    @Schema(description = "目标业务域编码")
    @NotBlank(message = "业务域编码不能为空")
    private String domainCode;

    @Schema(description = "目标租户ID；为空时使用当前登录租户")
    private Long targetTenantId;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "历史模板分类ID，业务域替换后前台不再使用")
    private Long templateCategoryId;

    @Schema(description = "模板ID列表。选择具体模板导入时传入")
    private List<Long> templateIds;

    @Schema(description = "流程管理员用户名列表。为空时沿用模板管理员配置")
    private List<String> adminUsers;
}
