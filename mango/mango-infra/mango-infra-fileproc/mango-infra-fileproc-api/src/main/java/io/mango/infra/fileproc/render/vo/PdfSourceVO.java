package io.mango.infra.fileproc.render.vo;

import io.mango.common.result.Require;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PDF 输入源。
 *
 * @param name 输入名称。
 * @param inputStream PDF 输入流。
 * @param sourcePath PDF 源文件路径。
 */
public record PdfSourceVO(String name, InputStream inputStream, Path sourcePath) {

    public PdfSourceVO {
        Require.isTrue(inputStream != null || sourcePath != null, "PDF 输入流或源文件路径不能为空");
    }

    public PdfSourceVO(String name, InputStream inputStream) {
        this(name, inputStream, null);
    }

    public static PdfSourceVO ofPath(Path sourcePath) {
        return new PdfSourceVO(sourcePath == null ? null : sourcePath.getFileName().toString(), null, sourcePath);
    }

    @Override
    public InputStream inputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        try {
            return Files.newInputStream(sourcePath);
        } catch (IOException ex) {
            throw new IllegalStateException("打开 PDF 源文件失败: " + sourcePath, ex);
        }
    }
}
