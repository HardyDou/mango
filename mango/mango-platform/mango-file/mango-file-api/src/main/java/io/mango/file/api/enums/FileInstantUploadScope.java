package io.mango.file.api.enums;

/**
 * 秒传匹配范围。
 */
public enum FileInstantUploadScope {

    /** 当前机构内匹配。 */
    TENANT,

    /** 全局匹配。 */
    GLOBAL;

    public static FileInstantUploadScope of(String value) {
        if (value == null || value.isBlank()) {
            return TENANT;
        }
        for (FileInstantUploadScope item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return TENANT;
    }
}
