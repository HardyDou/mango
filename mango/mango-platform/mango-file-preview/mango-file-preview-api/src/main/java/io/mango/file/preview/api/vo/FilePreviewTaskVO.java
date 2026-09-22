package io.mango.file.preview.api.vo;

import io.mango.file.preview.api.enums.FilePreviewTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/** 预览产物生成任务状态。 */
@Data
@Schema(description = "文件预览产物生成任务状态")
public class FilePreviewTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "原文件 ID")
    private Long fileId;
    @Schema(description = "预览任务状态")
    private FilePreviewTaskStatus status;
    @Schema(description = "转换进度，取值 0 到 100")
    private Integer progress;
    /** 当前任务前面等待的任务数量。 */
    @Schema(description = "当前任务前面等待的任务数量")
    private Integer queueAhead;
    /** 当前实例可同时处理的转换 worker 数量。 */
    @Schema(description = "当前实例可同时处理的转换 worker 数量")
    private Integer workerCount;
    @Schema(description = "面向用户的任务消息")
    private String message;
    @Schema(description = "已生成预览文件 ID")
    private Long previewFileId;
    @Schema(description = "任务创建时间")
    private Instant createdAt;
    @Schema(description = "任务更新时间")
    private Instant updatedAt;
}
