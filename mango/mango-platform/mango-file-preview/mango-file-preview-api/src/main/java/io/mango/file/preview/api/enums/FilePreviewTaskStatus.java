package io.mango.file.preview.api.enums;

/** 预览产物生成状态。 */
public enum FilePreviewTaskStatus {
    /** 文件超过自动预览阈值，等待用户确认。 */
    CONFIRM_REQUIRED,
    QUEUED,
    PROCESSING,
    /** 预览产物已就绪。 */
    SUCCEEDED,
    /** 转换引擎执行失败，表示本该能转换却失败了，需要排障。 */
    FAILED,
    /** 格式本身不支持在线预览，与转换失败无关，应引导用户下载原文件。 */
    NOT_SUPPORTED
}
