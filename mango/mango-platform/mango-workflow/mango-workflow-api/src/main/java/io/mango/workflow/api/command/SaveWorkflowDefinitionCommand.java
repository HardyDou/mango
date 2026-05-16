package io.mango.workflow.api.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 保存流程定义命令。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "保存流程定义命令")
public class SaveWorkflowDefinitionCommand {

    @Schema(description = "流程定义ID，新增时为空，修改时必填")
    private Long id;

    @Schema(description = "流程分组ID")
    @NotNull(message = "流程分组ID不能为空")
    private Long groupId;

    @Schema(description = "流程管理员用户名列表；审批人为空且策略为转交管理员时优先使用")
    private List<String> adminUsers;

    @Schema(description = "流程图标")
    @Size(max = 512, message = "流程图标最多512个字符")
    private String icon;

    @Schema(description = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    @Size(max = 128, message = "流程名称最多128个字符")
    private String definitionName;

    @Schema(description = "流程编码，对应 Flowable process id，必须唯一")
    @NotBlank(message = "流程编码不能为空")
    @Size(max = 128, message = "流程编码最多128个字符")
    private String definitionKey;

    @Schema(description = "设计器JSON内容，由前端流程设计器生成")
    @NotBlank(message = "设计器JSON不能为空")
    private String designerJson;

    @Schema(description = "BPMN XML内容，兼容调试字段，正式发布时由后端根据设计器JSON生成")
    private String bpmnXml;

    @Schema(description = "表单编码，用于业务表单关联")
    @Size(max = 128, message = "表单编码最多128个字符")
    private String formCode;

    @Schema(description = "动态表单JSON配置，保存流程发起或审批表单字段定义")
    private String formJson;

    @Schema(description = "流程状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-停用")
    private String status;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
