package io.mango.infra.fileproc.convert.convert;

/**
 * 格式转换参数键。
 */
public final class ConvertOptionKeys {

    /**
     * Office 或 PDF 打开密码。
     */
    public static final String PASSWORD = "password";

    /**
     * Office 转 PDF 页码范围。
     */
    public static final String PAGE_RANGE = "pageRange";

    /**
     * Office 转 PDF 水印文本。
     */
    public static final String WATERMARK = "watermark";

    /**
     * Office 转 PDF 图片质量，取值 1-100。
     */
    public static final String QUALITY = "quality";

    /**
     * Office 转 PDF 最大图片分辨率。
     */
    public static final String MAX_IMAGE_RESOLUTION = "maxImageResolution";

    /**
     * Office 转 PDF 时是否导出书签。
     */
    public static final String EXPORT_BOOKMARKS = "exportBookmarks";

    /**
     * Office 转 PDF 时是否导出批注。
     */
    public static final String EXPORT_NOTES = "exportNotes";

    /**
     * PDF 转图片 DPI。
     */
    public static final String DPI = "dpi";

    private ConvertOptionKeys() {
    }
}
