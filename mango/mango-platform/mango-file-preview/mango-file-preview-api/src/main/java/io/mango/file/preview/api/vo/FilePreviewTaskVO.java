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

    private Long fileId;
    private FilePreviewTaskStatus status;
    private Integer progress;
    /** 当前任务前面等待的任务数量。 */
    private Integer queueAhead;
    /** 当前实例可同时处理的转换 worker 数量。 */
    private Integer workerCount;
    private String message;
    private Long previewFileId;
    private Instant createdAt;
    private Instant updatedAt;
}
