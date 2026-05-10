package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程发布结果。
 */
@Data
@Schema(description = "流程发布结果")
public class WorkflowDeployVO {

    @Schema(description = "Flowable 部署ID")
    private String deploymentId;

    @Schema(description = "Flowable 流程定义ID")
    private String processDefinitionId;

    @Schema(description = "Flowable 流程定义版本")
    private Integer processDefinitionVersion;

    @Schema(description = "Mango发布版本号")
    private Integer versionNo;
}
