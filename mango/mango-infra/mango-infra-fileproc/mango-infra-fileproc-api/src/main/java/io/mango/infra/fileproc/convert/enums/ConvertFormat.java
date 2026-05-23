package io.mango.infra.fileproc.convert.enums;

import java.util.Locale;
import java.util.Optional;

public enum ConvertFormat {

    /**
     * 纯文本。
     */
    TEXT("text/plain", "txt"),
    /**
     * HTML 文本。
     */
    HTML("text/html", "html"),
    /**
     * Word 97-2003 文档。
     */
    DOC("application/msword", "doc"),
    /**
     * Word OpenXML 文档。
     */
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    /**
     * Excel 97-2003 表格。
     */
    XLS("application/vnd.ms-excel", "xls"),
    /**
     * Excel OpenXML 表格。
     */
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    /**
     * PowerPoint 97-2003 演示文稿。
     */
    PPT("application/vnd.ms-powerpoint", "ppt"),
    /**
     * PowerPoint OpenXML 演示文稿。
     */
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
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
     * TIFF 图像。
     */
    TIFF("image/tiff", "tif"),
    /**
     * ZIP 压缩包。
     */
    ZIP("application/zip", "zip");

    private final String contentType;

    private final String extension;

    ConvertFormat(String contentType, String extension) {
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
    public static Optional<ConvertFormat> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("JPG".equals(normalized)) {
            return Optional.of(JPEG);
        }
        for (ConvertFormat format : values()) {
            if (format.name().equals(normalized) || format.extension.equalsIgnoreCase(value)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
