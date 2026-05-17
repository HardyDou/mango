package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件逻辑目录。
 */
@Data
@Schema(description = "文件逻辑目录")
public class FileDirectoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目录ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long tenantId;

    @Schema(description = "父目录ID。根目录为0")
    private Long parentId;

    @Schema(description = "目录名称")
    private String directoryName;

    @Schema(description = "目录路径")
    private String directoryPath;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "子目录")
    private List<FileDirectoryVO> children = new ArrayList<>();
}
