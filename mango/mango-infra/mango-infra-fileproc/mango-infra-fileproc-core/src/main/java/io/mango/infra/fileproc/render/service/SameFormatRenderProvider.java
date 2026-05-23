package io.mango.infra.fileproc.render.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

import java.io.IOException;

/**
 * 同格式直通复制渲染器。
 */
public class SameFormatRenderProvider implements IRenderProvider {

    @Override
    public boolean supports(RenderFormat sourceFormat, RenderFormat targetFormat) {
        return sourceFormat != null && sourceFormat == targetFormat;
    }

    @Override
    public RenderResultVO render(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        try {
            return RenderResultVO.builder()
                    .format(command.targetFormat())
                    .fileName(resolveFileName(command))
                    .contentType(command.targetFormat().contentType())
                    .content(command.inputStream().readAllBytes())
                    .build();
        } catch (IOException ex) {
            throw new RenderToolException("同格式内容渲染失败", ex);
        }
    }

    private String resolveFileName(RenderCommand command) {
        if (command.fileName() == null || command.fileName().isBlank()) {
            return null;
        }
        String fileName = command.fileName().trim();
        String extension = "." + command.targetFormat().extension();
        if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
            return fileName;
        }
        return fileName + extension;
    }
}
