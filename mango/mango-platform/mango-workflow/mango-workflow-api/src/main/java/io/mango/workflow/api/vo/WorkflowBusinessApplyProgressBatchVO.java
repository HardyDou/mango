package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 批量业务申请进度结果。 */
@Data
@Schema(description = "批量业务申请进度结果")
public class WorkflowBusinessApplyProgressBatchVO {

    @Schema(description = "最新申请进度列表")
    private List<WorkflowBusinessApplyProgressVO> records;
}
