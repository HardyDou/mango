package io.mango.infra.fileproc.render.vo;

import java.util.Arrays;

/**
 * PDF 操作结果。
 *
 * @param fileName 输出文件名。
 * @param content 输出内容。
 */
public record PdfOperationResultVO(String fileName, byte[] content) {

    public PdfOperationResultVO {
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
