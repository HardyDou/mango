package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "修改岗位命令")
public class UpdatePostCommand {

    @NotNull(message = "岗位ID不能为空")
    @Schema(description = "岗位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "岗位名称不能为空")
    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String postName;

    @NotBlank(message = "岗位编码不能为空")
    @Schema(description = "岗位编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String postCode;

    @Schema(description = "排序值")
    private Integer postSort;

    @Schema(description = "岗位状态：0-禁用，1-启用")
    private String postStatus;

    @Schema(description = "备注")
    private String remark;
}
