package io.mango.infra.fileproc.compress.service;

import io.mango.infra.fileproc.compress.enums.FileCompression;

/**
 * 图片压缩档位参数。
 *
 * @param compression 压缩档位。
 * @param imageQuality 图片质量，范围 0-1。
 * @param maxSidePixels 图片最大边像素。
 * @param pdfRenderDpi PDF 页面渲染 DPI。
 */
public record CompressionSettings(
        FileCompression compression,
        float imageQuality,
        int maxSidePixels,
        float pdfRenderDpi) {

    public static CompressionSettings of(FileCompression compression) {
        FileCompression resolved = compression == null ? FileCompression.NONE : compression;
        return switch (resolved) {
            case LOW -> new CompressionSettings(resolved, 0.85F, 2000, 200F);
            case STANDARD -> new CompressionSettings(resolved, 0.80F, 1700, 150F);
            case MEDIUM -> new CompressionSettings(resolved, 0.72F, 1450, 120F);
            case HIGH -> new CompressionSettings(resolved, 0.62F, 1200, 96F);
            case EXTREME -> new CompressionSettings(resolved, 0.50F, 900, 60F);
            case NONE -> new CompressionSettings(resolved, 1.0F, Integer.MAX_VALUE, 72F);
        };
    }
}
