package io.mango.org.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新增岗位命令")
public class CreatePostCommand {

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 100, message = "岗位名称长度不能超过100个字符")
    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String postName;

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 50, message = "岗位编码长度不能超过50个字符")
    @Schema(description = "岗位编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String postCode;

    @Schema(description = "排序值")
    @PositiveOrZero(message = "排序值不能小于0")
    private Integer postSort;

    @Schema(description = "岗位状态：0-禁用，1-启用")
    @Pattern(regexp = "[01]", message = "岗位状态只能为0或1")
    private String postStatus;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
