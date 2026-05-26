package io.mango.file.api.enums;

/**
 * 文件记录状态。
 */
public enum FileRecordStatus {

    /** 上传中。 */
    UPLOADING(0),

    /** 已完成。 */
    COMPLETED(1),

    /** 已失败。 */
    FAILED(2),

    /** 已归档。 */
    ARCHIVED(9),

    /** 已删除。 */
    DELETED(10);

    private final int value;

    FileRecordStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
