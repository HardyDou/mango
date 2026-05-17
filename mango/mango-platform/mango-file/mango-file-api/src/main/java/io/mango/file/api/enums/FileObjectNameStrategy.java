package io.mango.file.api.enums;

/**
 * 文件对象命名策略。
 */
public enum FileObjectNameStrategy {

    /** 按日期路径加 UUID 命名。 */
    DATE_UUID,

    /** 按内容哈希命名。 */
    HASH,

    /** 保留原始文件名。 */
    ORIGINAL;

    public static FileObjectNameStrategy of(String value) {
        if (value == null || value.isBlank()) {
            return DATE_UUID;
        }
        for (FileObjectNameStrategy item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return DATE_UUID;
    }
}
