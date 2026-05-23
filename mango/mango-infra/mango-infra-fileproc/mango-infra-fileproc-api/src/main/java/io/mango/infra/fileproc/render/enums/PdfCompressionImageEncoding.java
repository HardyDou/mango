package io.mango.infra.fileproc.render.enums;

/**
 * PDF 压缩时的图片编码策略。
 */
public enum PdfCompressionImageEncoding {

    /**
     * 保持原始图片编码。
     */
    UNCHANGED,

    /**
     * 使用 JPEG 编码，适合照片类图片，压缩率较高。
     */
    JPEG,

    /**
     * 使用 Flate 编码，适合图表、截图等色块较多的图片。
     */
    FLATE,

    /**
     * 使用 JPEG2000 编码。
     */
    JPEG2000
}
