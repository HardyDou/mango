package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存文件逻辑目录命令。
 */
@Data
@Schema(description = "保存文件逻辑目录命令")
public class SaveFileDirectoryCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目录ID。新增时为空，修改时必填")
    private Long id;

    @Schema(description = "父目录ID。根目录为0")
    private Long parentId;

    @NotBlank(message = "目录名称不能为空")
    @Size(max = 128, message = "目录名称不能超过128个字符")
    @Schema(description = "目录名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String directoryName;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
