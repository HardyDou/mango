package io.mango.infra.fileproc.render.enums;

/**
 * PDF 压缩预设。
 */
public enum PdfCompressionPreset {

    /**
     * 默认压缩，兼顾质量、体积和兼容性。
     */
    DEFAULT,

    /**
     * 低强度压缩，尽量保持原始质量。
     */
    LOW,

    /**
     * 中等强度压缩。
     */
    MEDIUM,

    /**
     * 高强度压缩，优先减小体积。
     */
    HIGH,

    /**
     * 仅做结构优化，不压缩或缩放图片。
     */
    STRUCTURE_ONLY,

    /**
     * 自定义参数，不主动补充图片压缩默认值。
     */
    CUSTOM
}
