package io.mango.file.api.enums;

/**
 * 文件访问模式。
 */
public enum FileAccessMode {

    /** 通过 Java 文件服务转发访问。 */
    PROXY,

    /** 直接访问底层存储公开地址或签名地址。 */
    DIRECT;

    public static FileAccessMode of(String value) {
        if (value == null || value.isBlank()) {
            return PROXY;
        }
        for (FileAccessMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return PROXY;
    }
}
