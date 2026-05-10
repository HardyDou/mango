package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义发布版本视图。
 */
@Data
@Schema(description = "流程定义发布版本视图")
public class WorkflowDefinitionVersionVO {

    @Schema(description = "发布版本ID")
    private Long id;

    @Schema(description = "流程定义ID")
    private Long definitionId;

    @Schema(description = "Mango发布版本号")
    private Integer versionNo;

    @Schema(description = "设计器JSON快照")
    private String designerJson;

    @Schema(description = "动态表单JSON快照")
    private String formJson;

    @Schema(description = "BPMN XML快照")
    private String bpmnXml;

    @Schema(description = "Flowable部署ID")
    private String deploymentId;

    @Schema(description = "Flowable流程定义ID")
    private String processDefinitionId;

    @Schema(description = "Flowable流程定义版本")
    private Integer processDefinitionVersion;

    @Schema(description = "发布状态：SUCCESS-成功，FAILED-失败")
    private String publishStatus;

    @Schema(description = "发布说明或失败原因")
    private String publishMessage;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
