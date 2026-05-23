package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTML 转纯文本转换器。
 */
public class HtmlToTextConverter implements IConvertProvider {

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return sourceFormat == ConvertFormat.HTML && targetFormat == ConvertFormat.TEXT;
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
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
            return ConvertResultVO.builder()
                    .format(ConvertFormat.TEXT)
                    .fileName(resolveFileName(command))
                    .contentType(ConvertFormat.TEXT.contentType())
                    .content(text.getBytes(StandardCharsets.UTF_8))
                    .build();
        } catch (IOException ex) {
            throw new ConvertToolException("HTML 转文本失败", ex);
        }
    }

    private String resolveFileName(ConvertCommand command) {
        if (command.fileName() == null || command.fileName().isBlank()) {
            return null;
        }
        String fileName = command.fileName().trim();
        int index = fileName.lastIndexOf('.');
        String baseName = index > 0 ? fileName.substring(0, index) : fileName;
        return baseName + ".txt";
    }
}
