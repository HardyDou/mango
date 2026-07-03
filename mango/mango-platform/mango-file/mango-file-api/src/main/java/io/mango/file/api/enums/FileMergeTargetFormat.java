package io.mango.file.api.enums;

import java.util.Locale;

/**
 * 文件合并输出格式。
 */
public enum FileMergeTargetFormat {

    /** PDF 文档。 */
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String extension;

    FileMergeTargetFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    /**
     * 解析目标格式。空值默认 PDF。
     *
     * @param value 目标格式名称。
     * @return 目标格式。
     */
    public static FileMergeTargetFormat of(String value) {
        if (value == null || value.isBlank()) {
            return PDF;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (FileMergeTargetFormat format : values()) {
            if (format.name().equals(normalized) || format.extension.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("不支持的文件合并目标格式: " + value);
    }
}
