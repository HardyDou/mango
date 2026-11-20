package io.mango.file.preview.core.service.model;

import java.util.Locale;
import java.util.Set;

/**
 * 文件预览渲染方式。
 *
 * <p>只有原生 PDF 和需要平台转换为 PDF 的 Office 文档走平台自身的 PDF.js 页面，
 * 其余格式一律交给预览引擎按文件类型渲染，避免把引擎已支持的类型误送进 PDF 转换。</p>
 */
public enum FilePreviewMode {

    /** 原生 PDF，直接读取原文件交给 PDF.js。 */
    NATIVE_PDF,

    /** 需要先转换出 PDF 产物的 Office 文档。 */
    PDF_CONVERSION,

    /** 由预览引擎直接渲染，包括压缩包、图片、文本、媒体和表格等。 */
    ENGINE;

    private static final Set<String> PDF_CONVERSION_EXTENSIONS = Set.of(
            "doc", "docx", "ppt", "pptx", "odt", "ods", "odp");
    /**
     * 引擎用前端表格渲染的扩展名。该渲染方式要求引擎按对外 URL 回源取数，进程内直取通道下没有对外 URL，
     * 因此直取生效时与 {@code xls} 一致回落平台 PDF 转换。
     */
    private static final Set<String> ENGINE_SHEET_EXTENSIONS = Set.of("xlsx", "csv");
    private static final String PDF_EXTENSION = "pdf";

    /**
     * 按文件扩展名判断预览方式。
     *
     * @param extension 文件扩展名，大小写不敏感。
     * @return 预览方式；扩展名缺失时交给预览引擎判定。
     */
    public static FilePreviewMode of(String extension) {
        String normalized = normalize(extension);
        if (PDF_EXTENSION.equals(normalized)) {
            return NATIVE_PDF;
        }
        if (PDF_CONVERSION_EXTENSIONS.contains(normalized)) {
            return PDF_CONVERSION;
        }
        return ENGINE;
    }

    /**
     * 按文件扩展名与进程内直取能力判断预览方式。
     *
     * <p>表格（{@code xlsx}/{@code csv}）在引擎里靠前端表格渲染，需要按对外 URL 回源取数；进程内直取生效时
     * 没有对外 URL，该渲染方式不可用，因此回落 {@link #PDF_CONVERSION}。其余判定与 {@link #of(String)} 相同。</p>
     *
     * @param extension 文件扩展名，大小写不敏感。
     * @param internalSourceAvailable 进程内是否具备预览源文件直取能力；调用方传入与渲染分派一致的值
     * @return 预览方式
     */
    public static FilePreviewMode of(String extension, boolean internalSourceAvailable) {
        FilePreviewMode mode = of(extension);
        if (mode == ENGINE && internalSourceAvailable && ENGINE_SHEET_EXTENSIONS.contains(normalize(extension))) {
            return PDF_CONVERSION;
        }
        return mode;
    }

    /**
     * 解析文件扩展名。文件记录上的扩展名优先，缺失时回退到文件名。
     *
     * @param fileExtension 文件记录中的扩展名。
     * @param fileName 原始文件名。
     * @return 小写扩展名；无法解析时返回空字符串。
     */
    public static String extension(String fileExtension, String fileName) {
        String normalized = normalize(fileExtension);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? "" : normalize(fileName.substring(dot + 1));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
