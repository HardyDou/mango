package io.mango.infra.fileproc.render.vo;

import io.mango.common.contract.LocalCapabilityContract;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * PDF 操作结果。
 *
 * @param fileName 输出文件名。
 * @param content 输出内容。
 * @param outputPath 输出文件路径。
 */
@LocalCapabilityContract
public record PdfOperationResultVO(String fileName, byte[] content, Path outputPath) {

    public PdfOperationResultVO {
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    public PdfOperationResultVO(String fileName, byte[] content) {
        this(fileName, content, null);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    public boolean hasOutputPath() {
        return outputPath != null;
    }
}
