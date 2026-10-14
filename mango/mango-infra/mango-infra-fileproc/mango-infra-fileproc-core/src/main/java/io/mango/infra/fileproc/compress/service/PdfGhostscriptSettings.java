package io.mango.infra.fileproc.compress.service;

import io.mango.infra.fileproc.compress.enums.FileCompression;

import java.util.Optional;

/**
 * PDF 压缩的 Ghostscript 参数。
 *
 * @param pdfSettings Ghostscript {@code -dPDFSETTINGS} 取值。
 * @param imageResolution 彩色、灰度与单色图像的降采样分辨率，单位 DPI。
 * @param jpegQuality JPEG 编码质量，范围 0-100。
 */
record PdfGhostscriptSettings(String pdfSettings, int imageResolution, int jpegQuality) {

    private static final String EBOOK = "/ebook";
    private static final int LOW_RESOLUTION = 200;
    private static final int LOW_JPEG_QUALITY = 85;
    private static final int STANDARD_RESOLUTION = 150;
    private static final int STANDARD_JPEG_QUALITY = 80;
    private static final int MEDIUM_RESOLUTION = 120;
    private static final int MEDIUM_JPEG_QUALITY = 72;
    private static final int HIGH_RESOLUTION = 96;
    private static final int HIGH_JPEG_QUALITY = 62;
    private static final int EXTREME_RESOLUTION = 60;
    private static final int EXTREME_JPEG_QUALITY = 50;
    private static final int UNCOMPRESSED_RESOLUTION = 300;
    private static final int UNCOMPRESSED_JPEG_QUALITY = 90;

    static PdfGhostscriptSettings of(FileCompression compression) {
        FileCompression resolved = compression == null ? FileCompression.NONE : compression;
        return switch (resolved) {
            case LOW -> new PdfGhostscriptSettings(EBOOK, LOW_RESOLUTION, LOW_JPEG_QUALITY);
            case STANDARD -> new PdfGhostscriptSettings(EBOOK, STANDARD_RESOLUTION, STANDARD_JPEG_QUALITY);
            case MEDIUM -> new PdfGhostscriptSettings(EBOOK, MEDIUM_RESOLUTION, MEDIUM_JPEG_QUALITY);
            case HIGH -> new PdfGhostscriptSettings(EBOOK, HIGH_RESOLUTION, HIGH_JPEG_QUALITY);
            case EXTREME -> new PdfGhostscriptSettings(EBOOK, EXTREME_RESOLUTION, EXTREME_JPEG_QUALITY);
            case NONE -> new PdfGhostscriptSettings(EBOOK, UNCOMPRESSED_RESOLUTION, UNCOMPRESSED_JPEG_QUALITY);
        };
    }

    Optional<String> pdfSettingsArgument() {
        return Optional.of("-dPDFSETTINGS=" + pdfSettings);
    }
}
