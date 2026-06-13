package io.mango.workflow.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 确保流程定义已发布命令。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "确保流程定义已发布命令")
public class EnsureWorkflowDefinitionCommand {

    @Schema(description = "业务域编码")
    @NotBlank(message = "业务域编码不能为空")
    @Size(max = 64, message = "业务域编码最多64个字符")
    private String domainCode;

    @Schema(description = "流程分类编码")
    @NotBlank(message = "流程分类编码不能为空")
    @Size(max = 64, message = "流程分类编码最多64个字符")
    private String categoryCode;

    @Schema(description = "流程分类名称")
    @NotBlank(message = "流程分类名称不能为空")
    @Size(max = 128, message = "流程分类名称最多128个字符")
    private String categoryName;

    @Schema(description = "流程分类排序")
    @NotNull(message = "流程分类排序不能为空")
    private Integer categorySort;

    @Schema(description = "流程分类备注")
    @Size(max = 255, message = "流程分类备注最多255个字符")
    private String categoryRemark;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "流程管理员用户名列表")
    private List<String> adminUsers;

    @Schema(description = "流程图标")
    @Size(max = 512, message = "流程图标最多512个字符")
    private String icon;

    @Schema(description = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    @Size(max = 128, message = "流程名称最多128个字符")
    private String definitionName;

    @Schema(description = "流程编码")
    @NotBlank(message = "流程编码不能为空")
    @Size(max = 128, message = "流程编码最多128个字符")
    private String definitionKey;

    @Schema(description = "设计器JSON内容")
    @NotBlank(message = "设计器JSON不能为空")
    private String designerJson;

    @Schema(description = "表单编码")
    @Size(max = 128, message = "表单编码最多128个字符")
    private String formCode;

    @Schema(description = "动态表单JSON配置")
    private String formJson;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
