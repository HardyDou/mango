package io.mango.infra.fileproc.compress.enums;

import java.util.Locale;

/**
 * 文件下载压缩档位。
 * <p>
 * 档位表示压缩强度，强度从低到高为 {@link #LOW}、{@link #STANDARD}、{@link #MEDIUM}、
 * {@link #HIGH}、{@link #EXTREME}；强度越高，输出体积越小、清晰度越低。
 */
public enum FileCompression {

    /** 不压缩。 */
    NONE,

    /** 轻度压缩，优先清晰度。 */
    LOW,

    /** 标准压缩。 */
    STANDARD,

    /** 中度压缩，平衡清晰度与体积。 */
    MEDIUM,

    /** 强压缩，优先体积。 */
    HIGH,

    /** 极限压缩，体积最小、清晰度最低。 */
    EXTREME;

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
