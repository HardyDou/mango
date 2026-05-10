package io.mango.file.api.enums;

/**
 * 文件访问级别。
 */
public enum FileAccessLevel {

    /** 仅所属机构或授权主体可访问。 */
    PRIVATE,

    /** 公开读取，维护仍需授权。 */
    PUBLIC_READ,

    /** 仅系统内部使用。 */
    INTERNAL;

    public static FileAccessLevel of(String value) {
        if (value == null || value.isBlank()) {
            return PRIVATE;
        }
        for (FileAccessLevel item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item;
            }
        }
        return PRIVATE;
    }
}
