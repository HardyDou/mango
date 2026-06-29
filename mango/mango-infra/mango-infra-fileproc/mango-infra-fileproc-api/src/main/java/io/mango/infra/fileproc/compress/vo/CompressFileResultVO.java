package io.mango.infra.fileproc.compress.vo;

import java.util.Arrays;

/**
 * 文件压缩结果。
 *
 * @param fileName 输出文件名。
 * @param contentType 输出内容类型。
 * @param content 输出内容。
 * @param originalSize 原始大小。
 * @param compressedSize 压缩后大小。
 * @param targetSize 目标大小。
 * @param targetReached 是否达到目标大小。
 */
public record CompressFileResultVO(
        String fileName,
        String contentType,
        byte[] content,
        long originalSize,
        long compressedSize,
        Long targetSize,
        boolean targetReached) {

    public CompressFileResultVO {
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
