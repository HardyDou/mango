package io.mango.file.api.enums;

/**
 * 物理文件对象状态。
 */
public enum FileObjectStatus {

    /** 上传中。 */
    UPLOADING(0),

    /** 可用。 */
    COMPLETED(1),

    /** 失败。 */
    FAILED(2),

    /** 无业务引用。 */
    UNREFERENCED(3),

    /** 已删除。 */
    DELETED(9);

    private final int value;

    FileObjectStatus(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
