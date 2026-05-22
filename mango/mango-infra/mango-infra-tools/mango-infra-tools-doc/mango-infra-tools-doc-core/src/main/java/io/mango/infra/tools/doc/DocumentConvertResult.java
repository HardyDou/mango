package io.mango.infra.tools.doc;

import java.util.Arrays;
import java.util.Objects;

/**
 * Result of one local document conversion.
 */
public final class DocumentConvertResult {

    private final DocumentFormat format;

    private final String fileName;

    private final String contentType;

    private final byte[] content;

    private DocumentConvertResult(Builder builder) {
        this.format = Objects.requireNonNull(builder.format, "format must not be null");
        this.fileName = builder.fileName;
        this.contentType = builder.contentType == null ? builder.format.contentType() : builder.contentType;
        this.content = builder.content == null ? new byte[0] : Arrays.copyOf(builder.content, builder.content.length);
    }

    public DocumentFormat format() {
        return format;
    }

    public String fileName() {
        return fileName;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private DocumentFormat format;

        private String fileName;

        private String contentType;

        private byte[] content;

        private Builder() {
        }

        public Builder format(DocumentFormat format) {
            this.format = format;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder content(byte[] content) {
            this.content = content == null ? null : Arrays.copyOf(content, content.length);
            return this;
        }

        public DocumentConvertResult build() {
            return new DocumentConvertResult(this);
        }
    }
}
