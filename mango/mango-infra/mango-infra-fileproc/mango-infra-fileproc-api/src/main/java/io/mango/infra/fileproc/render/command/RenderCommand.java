package io.mango.infra.fileproc.render.command;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.enums.RenderFormat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档渲染命令。
 * <p>
 * 命令只描述渲染输入，不包含文件中心标识、存储位置、权限或租户信息。
 */
public final class RenderCommand {

    private final RenderFormat sourceFormat;

    private final RenderFormat targetFormat;

    private final InputStream inputStream;

    private final Path sourcePath;

    private final Path targetPath;

    private final String fileName;

    private final Map<String, Object> options;

    private final Map<String, Object> variables;

    private final List<RenderVariableDefinition> variableDefinitions;

    private RenderCommand(Builder builder) {
        Require.notNull(builder.sourceFormat, "源格式不能为空");
        Require.notNull(builder.targetFormat, "目标格式不能为空");
        Require.isTrue(builder.inputStream != null || builder.sourcePath != null, "渲染输入流或源文件路径不能为空");
        this.sourceFormat = builder.sourceFormat;
        this.targetFormat = builder.targetFormat;
        this.inputStream = builder.inputStream;
        this.sourcePath = builder.sourcePath;
        this.targetPath = builder.targetPath;
        this.fileName = builder.fileName;
        this.options = Collections.unmodifiableMap(new HashMap<>(builder.options));
        this.variables = Collections.unmodifiableMap(new HashMap<>(builder.variables));
        this.variableDefinitions = List.copyOf(builder.variableDefinitions);
    }

    public RenderFormat sourceFormat() {
        return sourceFormat;
    }

    public RenderFormat targetFormat() {
        return targetFormat;
    }

    public InputStream inputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        try {
            return Files.newInputStream(sourcePath);
        } catch (IOException ex) {
            throw new IllegalStateException("打开渲染源文件失败: " + sourcePath, ex);
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

    public Map<String, Object> variables() {
        return variables;
    }

    public List<RenderVariableDefinition> variableDefinitions() {
        return variableDefinitions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private RenderFormat sourceFormat;

        private RenderFormat targetFormat;

        private InputStream inputStream;

        private Path sourcePath;

        private Path targetPath;

        private String fileName;

        private final Map<String, Object> options = new HashMap<>();

        private final Map<String, Object> variables = new HashMap<>();

        private final List<RenderVariableDefinition> variableDefinitions = new ArrayList<>();

        private Builder() {
        }

        public Builder sourceFormat(RenderFormat sourceFormat) {
            this.sourceFormat = sourceFormat;
            return this;
        }

        public Builder targetFormat(RenderFormat targetFormat) {
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

        public Builder variable(String key, Object value) {
            if (key != null && !key.isBlank()) {
                this.variables.put(key, value);
            }
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            if (variables != null) {
                variables.forEach(this::variable);
            }
            return this;
        }

        public Builder variableDefinition(RenderVariableDefinition variableDefinition) {
            if (variableDefinition != null) {
                this.variableDefinitions.add(variableDefinition);
            }
            return this;
        }

        public Builder variableDefinitions(List<RenderVariableDefinition> variableDefinitions) {
            if (variableDefinitions != null) {
                variableDefinitions.forEach(this::variableDefinition);
            }
            return this;
        }

        public RenderCommand build() {
            return new RenderCommand(this);
        }
    }
}
