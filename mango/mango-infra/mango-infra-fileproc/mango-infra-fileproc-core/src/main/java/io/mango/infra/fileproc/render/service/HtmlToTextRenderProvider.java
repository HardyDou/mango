package io.mango.infra.fileproc.render.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTML 富文本渲染为纯文本。
 */
public class HtmlToTextRenderProvider implements IRenderProvider {

    @Override
    public boolean supports(RenderFormat sourceFormat, RenderFormat targetFormat) {
        return sourceFormat == RenderFormat.HTML && targetFormat == RenderFormat.TEXT;
    }

    @Override
    public RenderResultVO render(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        try {
            String html = new String(command.inputStream().readAllBytes(), StandardCharsets.UTF_8);
            String text = html
                    .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                    .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                    .replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?i)</p>", "\n")
                    .replaceAll("<[^>]+>", "")
                    .replace("&nbsp;", " ")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .trim();
            return RenderResultVO.builder()
                    .format(RenderFormat.TEXT)
                    .fileName(resolveFileName(command))
                    .contentType(RenderFormat.TEXT.contentType())
                    .content(text.getBytes(StandardCharsets.UTF_8))
                    .build();
        } catch (IOException ex) {
            throw new RenderToolException("HTML 渲染为文本失败", ex);
        }
    }

    private String resolveFileName(RenderCommand command) {
        if (command.fileName() == null || command.fileName().isBlank()) {
            return null;
        }
        String fileName = command.fileName().trim();
        int index = fileName.lastIndexOf('.');
        String baseName = index > 0 ? fileName.substring(0, index) : fileName;
        return baseName + ".txt";
    }
}
