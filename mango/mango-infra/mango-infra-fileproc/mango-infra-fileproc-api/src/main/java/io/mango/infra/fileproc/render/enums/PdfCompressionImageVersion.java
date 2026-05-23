package io.mango.infra.fileproc.render.enums;

/**
 * PDF 图片压缩算法版本。
 */
public enum PdfCompressionImageVersion {

    /**
     * 标准压缩算法。
     */
    STANDARD,

    /**
     * 优先压缩速度。
     */
    FAST,

    /**
     * 混合压缩算法。
     */
    MIXED
}
