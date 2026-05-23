package io.mango.infra.fileproc.convert.vo;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;

import java.util.Arrays;

/**
 * 格式转换结果。
 */
public final class ConvertResultVO {

    private final ConvertFormat format;

    private final String fileName;

    private final String contentType;

    private final byte[] content;

    private ConvertResultVO(Builder builder) {
        Require.notNull(builder.format, "转换结果格式不能为空");
        this.format = builder.format;
        this.fileName = builder.fileName;
        this.contentType = builder.contentType == null ? builder.format.contentType() : builder.contentType;
        this.content = builder.content == null ? new byte[0] : Arrays.copyOf(builder.content, builder.content.length);
    }

    public ConvertFormat format() {
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

        private ConvertFormat format;

        private String fileName;

        private String contentType;

        private byte[] content;

        private Builder() {
        }

        public Builder format(ConvertFormat format) {
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

        public ConvertResultVO build() {
            return new ConvertResultVO(this);
        }
    }
}
