package io.mango.infra.tools.doc;

import java.util.Locale;
import java.util.Optional;

/**
 * Document format supported by local document tools.
 */
public enum DocumentFormat {

    TEXT("text/plain", "txt"),
    HTML("text/html", "html"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    PDF("application/pdf", "pdf"),
    OFD("application/ofd", "ofd"),
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    ZIP("application/zip", "zip");

    private final String contentType;

    private final String extension;

    DocumentFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<DocumentFormat> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("JPG".equals(normalized)) {
            return Optional.of(JPEG);
        }
        for (DocumentFormat format : values()) {
            if (format.name().equals(normalized) || format.extension.equalsIgnoreCase(value)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
