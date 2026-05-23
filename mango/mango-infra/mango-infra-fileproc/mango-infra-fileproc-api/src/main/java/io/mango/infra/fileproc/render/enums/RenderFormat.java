package io.mango.infra.fileproc.render.enums;

import java.util.Locale;
import java.util.Optional;

/**
 * 文档渲染格式。
 */
public enum RenderFormat {

    /**
     * 纯文本。
     */
    TEXT("text/plain", "txt"),

    /**
     * HTML 富文本。
     */
    HTML("text/html", "html"),

    /**
     * Word OpenXML 文档。
     */
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),

    /**
     * Excel OpenXML 表格。
     */
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),

    /**
     * PDF 文档。
     */
    PDF("application/pdf", "pdf"),

    /**
     * OFD 文档。
     */
    OFD("application/ofd", "ofd"),

    /**
     * PNG 图片。
     */
    PNG("image/png", "png"),

    /**
     * JPEG 图片。
     */
    JPEG("image/jpeg", "jpg"),

    /**
     * ZIP 压缩包。
     */
    ZIP("application/zip", "zip");

    private final String contentType;

    private final String extension;

    RenderFormat(String contentType, String extension) {
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
     * 按枚举名或文件扩展名解析格式。
     *
     * @param value 枚举名或文件扩展名，大小写不敏感。
     * @return 可识别时返回格式，否则返回空。
     */
    public static Optional<RenderFormat> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("JPG".equals(normalized)) {
            return Optional.of(JPEG);
        }
        for (RenderFormat format : values()) {
            if (format.name().equals(normalized) || format.extension.equalsIgnoreCase(value)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
