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

    private static final float LOW_QUALITY = 0.85F;
    private static final int LOW_MAX_SIDE_PIXELS = 2000;
    private static final float LOW_RENDER_DPI = 200F;
    private static final float STANDARD_QUALITY = 0.80F;
    private static final int STANDARD_MAX_SIDE_PIXELS = 1700;
    private static final float STANDARD_RENDER_DPI = 150F;
    private static final float MEDIUM_QUALITY = 0.72F;
    private static final int MEDIUM_MAX_SIDE_PIXELS = 1450;
    private static final float MEDIUM_RENDER_DPI = 120F;
    private static final float HIGH_QUALITY = 0.62F;
    private static final int HIGH_MAX_SIDE_PIXELS = 1200;
    private static final float HIGH_RENDER_DPI = 96F;
    private static final float EXTREME_QUALITY = 0.50F;
    private static final int EXTREME_MAX_SIDE_PIXELS = 900;
    private static final float EXTREME_RENDER_DPI = 60F;
    private static final float UNCOMPRESSED_QUALITY = 1.0F;
    private static final float UNCOMPRESSED_RENDER_DPI = 72F;

    public static CompressionSettings of(FileCompression compression) {
        FileCompression resolved = compression == null ? FileCompression.NONE : compression;
        return switch (resolved) {
            case LOW -> new CompressionSettings(resolved, LOW_QUALITY, LOW_MAX_SIDE_PIXELS, LOW_RENDER_DPI);
            case STANDARD -> new CompressionSettings(resolved, STANDARD_QUALITY, STANDARD_MAX_SIDE_PIXELS,
                    STANDARD_RENDER_DPI);
            case MEDIUM -> new CompressionSettings(resolved, MEDIUM_QUALITY, MEDIUM_MAX_SIDE_PIXELS,
                    MEDIUM_RENDER_DPI);
            case HIGH -> new CompressionSettings(resolved, HIGH_QUALITY, HIGH_MAX_SIDE_PIXELS, HIGH_RENDER_DPI);
            case EXTREME -> new CompressionSettings(resolved, EXTREME_QUALITY, EXTREME_MAX_SIDE_PIXELS,
                    EXTREME_RENDER_DPI);
            case NONE -> new CompressionSettings(resolved, UNCOMPRESSED_QUALITY, Integer.MAX_VALUE,
                    UNCOMPRESSED_RENDER_DPI);
        };
    }
}
