package io.mango.infra.fileproc.compress.enums;

import java.util.Locale;

/**
 * 文件下载压缩档位。
 */
public enum FileCompression {

    /** 不压缩。 */
    NONE,

    /** 轻度压缩，优先清晰度。 */
    LOW,

    /** 中度压缩，平衡清晰度与体积。 */
    MEDIUM,

    /** 强压缩，优先体积。 */
    HIGH;

    public static FileCompression of(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        return FileCompression.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean enabled() {
        return this != NONE;
    }
}
