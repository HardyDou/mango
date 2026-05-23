package io.mango.infra.fileproc.render.vo;

import java.util.Arrays;

/**
 * PDF 目标压缩结果。
 *
 * @param fileName 输出文件名。
 * @param content 输出内容。
 * @param originalSize 原始大小，单位字节。
 * @param compressedSize 压缩后大小，单位字节。
 * @param targetSize 目标大小，单位字节。
 * @param targetReached 是否达到目标大小。
 * @param finalImageQuality 最终图片质量。
 * @param finalResolution 最终最大图片分辨率 DPI。
 * @param iterations 实际压缩次数。
 */
public record PdfCompressionResultVO(
        String fileName,
        byte[] content,
        long originalSize,
        long compressedSize,
        long targetSize,
        boolean targetReached,
        int finalImageQuality,
        int finalResolution,
        int iterations) {

    public PdfCompressionResultVO {
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
