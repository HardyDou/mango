package io.mango.infra.fileproc.render.command;

import io.mango.common.result.Require;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PDF 水印命令。
 *
 * @param fileName 输出文件名。
 * @param inputStream PDF 输入流。
 * @param sourcePath PDF 源文件路径。
 * @param targetPath PDF 输出文件路径。
 * @param watermarkText 水印文本。
 */
public record AddPdfWatermarkCommand(
        String fileName,
        InputStream inputStream,
        Path sourcePath,
        Path targetPath,
        String watermarkText) {

    public AddPdfWatermarkCommand {
        Require.isTrue(inputStream != null || sourcePath != null, "PDF 输入流或源文件路径不能为空");
    }

    public AddPdfWatermarkCommand(String fileName, InputStream inputStream, String watermarkText) {
        this(fileName, inputStream, null, null, watermarkText);
    }

    public static AddPdfWatermarkCommand ofPath(Path sourcePath, Path targetPath, String watermarkText) {
        return new AddPdfWatermarkCommand(null, null, sourcePath, targetPath, watermarkText);
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
