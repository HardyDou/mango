package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/** 流程模板推送目标机构候选项。 */
@Data
@Schema(description = "流程模板推送目标机构候选项")
public class WorkflowTenantOptionVO implements Serializable {

    @Schema(description = "机构ID")
    private Long id;

    @Schema(description = "机构名称")
    private String tenantName;

    @Schema(description = "机构编码")
    private String tenantCode;
}
