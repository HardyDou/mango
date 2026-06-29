package io.mango.infra.fileproc.compress.command;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.compress.enums.FileCompression;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 文件压缩命令。
 *
 * @param fileName 文件名。
 * @param contentType 内容类型。
 * @param inputStream 文件输入流。
 * @param compression 压缩档位。
 * @param targetSizeBytes 单文件目标大小，单位字节。
 */
public record CompressFileCommand(
        String fileName,
        String contentType,
        InputStream inputStream,
        FileCompression compression,
        Long targetSizeBytes) {

    public CompressFileCommand {
        Require.notNull(inputStream, "文件压缩输入流不能为空");
        if (targetSizeBytes != null) {
            Require.isTrue(targetSizeBytes > 0, "文件压缩目标大小必须大于 0");
        }
    }

    public byte[] readAllBytes() {
        try {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("读取文件压缩输入失败", ex);
        }
    }

    public FileCompression resolvedCompression() {
        return compression == null ? FileCompression.NONE : compression;
    }

    public String normalizedContentType() {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }
}
