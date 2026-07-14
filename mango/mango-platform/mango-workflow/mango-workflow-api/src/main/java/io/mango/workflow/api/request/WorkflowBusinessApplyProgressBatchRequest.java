package io.mango.workflow.api.request;

import io.mango.workflow.api.validation.WorkflowOptionalValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 批量查询业务申请进度请求。 */
@Data
@Schema(description = "业务工作流申请批量进度请求")
public class WorkflowBusinessApplyProgressBatchRequest {

    @Schema(description = "业务类型")
    @NotNull(groups = WorkflowOptionalValidation.class)
    @Size(max = 128, message = "业务类型最多128个字符")
    private String businessType;

    @Schema(description = "业务主键集合")
    @NotEmpty(message = "业务主键集合不能为空")
    private List<@Size(max = 128, message = "业务主键最多128个字符") String> businessKeys;
}
