package io.mango.workflow.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 业务类型与业务主键批量查询条件。 */
@Data
@Schema(description = "业务类型与业务主键批量查询条件")
public class WorkflowBusinessTypeKeysQuery {

    @Schema(description = "业务类型")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 128, message = "业务类型最多128个字符")
    private String businessType;

    @Schema(description = "业务主键集合")
    @NotEmpty(message = "业务主键集合不能为空")
    private List<@Size(max = 128, message = "业务主键最多128个字符") String> businessKeys;
}
