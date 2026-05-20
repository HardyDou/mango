package io.mango.file.api.enums;

/**
 * 文件上传会话状态。
 */
public enum FileUploadSessionStatus {

    /** 已初始化。 */
    INIT,

    /** 上传中。 */
    UPLOADING,

    /** 完成中。 */
    COMPLETING,

    /** 已完成。 */
    COMPLETED,

    /** 失败。 */
    FAILED,

    /** 已取消。 */
    ABORTED,

    /** 已过期。 */
    EXPIRED;
}
