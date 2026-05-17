package io.mango.file.api.enums;

/**
 * 文件重名处理策略。
 */
public enum FileDuplicateNameStrategy {

    /** 同目录重名时拒绝上传。 */
    REJECT,

    /** 同目录重名时自动重命名。 */
    AUTO_RENAME,

    /** 允许同目录存在重复文件名。 */
    ALLOW;

    public static FileDuplicateNameStrategy of(String value) {
        if (value == null || value.isBlank()) {
            return REJECT;
        }
        for (FileDuplicateNameStrategy item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return REJECT;
    }
}
