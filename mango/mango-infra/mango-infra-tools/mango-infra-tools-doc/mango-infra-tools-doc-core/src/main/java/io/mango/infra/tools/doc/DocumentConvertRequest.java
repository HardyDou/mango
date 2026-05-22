package io.mango.infra.tools.doc;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Request for one local document conversion.
 */
public final class DocumentConvertRequest {

    private final DocumentFormat sourceFormat;

    private final DocumentFormat targetFormat;

    private final InputStream inputStream;

    private final String fileName;

    private final Map<String, Object> options;

    private DocumentConvertRequest(Builder builder) {
        this.sourceFormat = Objects.requireNonNull(builder.sourceFormat, "sourceFormat must not be null");
        this.targetFormat = Objects.requireNonNull(builder.targetFormat, "targetFormat must not be null");
        this.inputStream = Objects.requireNonNull(builder.inputStream, "inputStream must not be null");
        this.fileName = builder.fileName;
        this.options = Collections.unmodifiableMap(new HashMap<>(builder.options));
    }

    public DocumentFormat sourceFormat() {
        return sourceFormat;
    }

    public DocumentFormat targetFormat() {
        return targetFormat;
    }

    public InputStream inputStream() {
        return inputStream;
    }

    public String fileName() {
        return fileName;
    }

    public Map<String, Object> options() {
        return options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private DocumentFormat sourceFormat;

        private DocumentFormat targetFormat;

        private InputStream inputStream;

        private String fileName;

        private final Map<String, Object> options = new HashMap<>();

        private Builder() {
        }

        public Builder sourceFormat(DocumentFormat sourceFormat) {
            this.sourceFormat = sourceFormat;
            return this;
        }

        public Builder targetFormat(DocumentFormat targetFormat) {
            this.targetFormat = targetFormat;
            return this;
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder option(String key, Object value) {
            if (key != null && !key.isBlank()) {
                this.options.put(key, value);
            }
            return this;
        }

        public Builder options(Map<String, Object> options) {
            if (options != null) {
                options.forEach(this::option);
            }
            return this;
        }

        public DocumentConvertRequest build() {
            return new DocumentConvertRequest(this);
        }
    }
}
