package io.mango.file.preview.api.enums;

/** 预览产物生成状态。 */
public enum FilePreviewTaskStatus {
    /** 文件超过自动预览阈值，等待用户确认。 */
    CONFIRM_REQUIRED,
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED
}
