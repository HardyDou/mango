package io.mango.infra.fileproc.render.command;

import io.mango.common.result.Require;

import java.io.InputStream;

/**
 * PDF 水印命令。
 *
 * @param fileName 输出文件名。
 * @param inputStream PDF 输入流。
 * @param watermarkText 水印文本。
 */
public record AddPdfWatermarkCommand(String fileName, InputStream inputStream, String watermarkText) {

    public AddPdfWatermarkCommand {
        Require.notNull(inputStream, "PDF 输入流不能为空");
    }
}
