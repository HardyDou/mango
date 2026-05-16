package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程定义视图。
 */
@Data
@Schema(description = "流程定义视图")
public class WorkflowDefinitionVO {

    @Schema(description = "流程定义ID")
    private Long id;

    @Schema(description = "流程分组ID")
    private Long groupId;

    @Schema(description = "流程分组名称")
    private String groupName;

    @Schema(description = "流程管理员用户名列表")
    private List<String> adminUsers;

    @Schema(description = "流程图标")
    private String icon;

    @Schema(description = "流程名称")
    private String definitionName;

    @Schema(description = "流程编码")
    private String definitionKey;

    @Schema(description = "Flowable 部署ID")
    private String deploymentId;

    @Schema(description = "Flowable 流程定义ID")
    private String processDefinitionId;

    @Schema(description = "Flowable 流程定义版本")
    private Integer processDefinitionVersion;

    @Schema(description = "Mango最近发布版本号")
    private Integer publishedVersionNo;

    @Schema(description = "设计器JSON内容")
    private String designerJson;

    @Schema(description = "BPMN XML 内容，最近一次发布生成的引擎产物")
    private String bpmnXml;

    @Schema(description = "表单编码")
    private String formCode;

    @Schema(description = "动态表单JSON配置")
    private String formJson;

    @Schema(description = "流程状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-停用")
    private String status;

    @Schema(description = "最后发布时间")
    private LocalDateTime lastDeployTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
