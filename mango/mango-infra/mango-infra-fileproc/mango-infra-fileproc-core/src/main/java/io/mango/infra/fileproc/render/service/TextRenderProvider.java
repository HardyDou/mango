package io.mango.infra.fileproc.render.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Text template renderer.
 */
public class TextRenderProvider implements IRenderProvider {

    private final FreemarkerRenderEngine freemarkerEngine;

    public TextRenderProvider(FreemarkerRenderEngine freemarkerEngine) {
        this.freemarkerEngine = freemarkerEngine;
    }

    @Override
    public boolean supports(RenderFormat sourceFormat, RenderFormat targetFormat) {
        return sourceFormat == RenderFormat.TEXT && targetFormat == RenderFormat.TEXT;
    }

    @Override
    public RenderResultVO render(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        String content = readContent(command);
        String rendered = freemarkerEngine.render(content, command.variables());
        return RenderResultVO.builder()
                .format(RenderFormat.TEXT)
                .fileName(resolveFileName(command))
                .contentType("text/plain;charset=UTF-8")
                .content(rendered.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Override
    public List<String> extractVariables(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        return freemarkerEngine.extract(readContent(command));
    }

    private String readContent(RenderCommand command) {
        try {
            return new String(command.inputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RenderToolException("读取文本模板失败", ex);
        }
    }

    private String resolveFileName(RenderCommand command) {
        if (command.fileName() == null || command.fileName().isBlank()) {
            return null;
        }
        String fileName = command.fileName().trim();
        String extension = "." + RenderFormat.TEXT.extension();
        if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
            return fileName;
        }
        return fileName + extension;
    }
}
