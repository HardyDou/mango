package io.mango.infra.fileproc.convert.command;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 格式转换命令。
 * <p>
 * 命令只描述转换输入，不包含文件中心标识、存储位置、权限或租户信息。
 */
public final class ConvertCommand {

    private final ConvertFormat sourceFormat;

    private final ConvertFormat targetFormat;

    private final InputStream inputStream;

    private final Path sourcePath;

    private final Path targetPath;

    private final String fileName;

    private final Map<String, Object> options;

    private ConvertCommand(Builder builder) {
        Require.notNull(builder.sourceFormat, "源格式不能为空");
        Require.notNull(builder.targetFormat, "目标格式不能为空");
        Require.isTrue(builder.inputStream != null || builder.sourcePath != null, "转换输入流或源文件路径不能为空");
        this.sourceFormat = builder.sourceFormat;
        this.targetFormat = builder.targetFormat;
        this.inputStream = builder.inputStream;
        this.sourcePath = builder.sourcePath;
        this.targetPath = builder.targetPath;
        this.fileName = builder.fileName;
        this.options = Collections.unmodifiableMap(new HashMap<>(builder.options));
    }

    public ConvertFormat sourceFormat() {
        return sourceFormat;
    }

    public ConvertFormat targetFormat() {
        return targetFormat;
    }

    public InputStream inputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        try {
            return Files.newInputStream(sourcePath);
        } catch (IOException ex) {
            throw new IllegalStateException("打开转换源文件失败: " + sourcePath, ex);
        }
    }

    public Path sourcePath() {
        return sourcePath;
    }

    public Path targetPath() {
        return targetPath;
    }

    public boolean hasInputStream() {
        return inputStream != null;
    }

    public boolean hasSourcePath() {
        return sourcePath != null;
    }

    public boolean hasTargetPath() {
        return targetPath != null;
    }

    public InputStream rawInputStream() {
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

        private ConvertFormat sourceFormat;

        private ConvertFormat targetFormat;

        private InputStream inputStream;

        private Path sourcePath;

        private Path targetPath;

        private String fileName;

        private final Map<String, Object> options = new HashMap<>();

        private Builder() {
        }

        public Builder sourceFormat(ConvertFormat sourceFormat) {
            this.sourceFormat = sourceFormat;
            return this;
        }

        public Builder targetFormat(ConvertFormat targetFormat) {
            this.targetFormat = targetFormat;
            return this;
        }

        public Builder inputStream(InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder sourcePath(Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder sourcePath(String sourcePath) {
            this.sourcePath = sourcePath == null || sourcePath.isBlank() ? null : Path.of(sourcePath);
            return this;
        }

        public Builder targetPath(Path targetPath) {
            this.targetPath = targetPath;
            return this;
        }

        public Builder targetPath(String targetPath) {
            this.targetPath = targetPath == null || targetPath.isBlank() ? null : Path.of(targetPath);
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

        public ConvertCommand build() {
            return new ConvertCommand(this);
        }
    }
}
