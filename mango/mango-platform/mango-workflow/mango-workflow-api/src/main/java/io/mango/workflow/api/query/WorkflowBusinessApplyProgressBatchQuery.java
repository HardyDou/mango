package io.mango.workflow.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 业务工作流申请批量进度查询。
 */
@Data
@Schema(description = "业务工作流申请批量进度查询")
public class WorkflowBusinessApplyProgressBatchQuery {

    @Schema(description = "业务类型")
    @Size(max = 128, message = "业务类型最多128个字符")
    private String businessType;

    @Schema(description = "业务主键集合")
    @NotEmpty(message = "业务主键集合不能为空")
    private List<@Size(max = 128, message = "业务主键最多128个字符") String> businessKeys;
}
