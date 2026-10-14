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

    static PdfGhostscriptSettings of(FileCompression compression) {
        FileCompression resolved = compression == null ? FileCompression.NONE : compression;
        return switch (resolved) {
            case LOW -> new PdfGhostscriptSettings(EBOOK, 200, 85);
            case STANDARD -> new PdfGhostscriptSettings(EBOOK, 150, 80);
            case MEDIUM -> new PdfGhostscriptSettings(EBOOK, 120, 72);
            case HIGH -> new PdfGhostscriptSettings(EBOOK, 96, 62);
            case EXTREME -> new PdfGhostscriptSettings(EBOOK, 60, 50);
            case NONE -> new PdfGhostscriptSettings(EBOOK, 300, 90);
        };
    }

    Optional<String> pdfSettingsArgument() {
        return Optional.of("-dPDFSETTINGS=" + pdfSettings);
    }
}
