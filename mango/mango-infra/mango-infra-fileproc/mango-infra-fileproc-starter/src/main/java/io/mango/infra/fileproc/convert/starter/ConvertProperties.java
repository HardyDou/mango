package io.mango.infra.fileproc.convert.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 格式转换工具配置。
 */
@ConfigurationProperties(prefix = "mango.fileproc.convert")
public class ConvertProperties {

    /**
     * 是否启用格式转换自动配置。
     */
    private boolean enabled = true;

    /**
     * 是否注册轻量 HTML 转文本转换器。
     */
    private boolean htmlToTextEnabled = true;

    /**
     * 是否注册 Office 转 PDF 转换器。
     */
    private boolean officeToPdfEnabled = true;

    /**
     * 是否注册 Aspose Word 转 PDF 转换器。
     */
    private boolean asposeWordToPdfEnabled = true;

    /**
     * 是否注册 Aspose Excel 转 PDF 转换器。
     */
    private boolean asposeExcelToPdfEnabled = true;

    /**
     * 是否注册 Aspose PPT 转 PDF 转换器。
     */
    private boolean asposeSlideToPdfEnabled = true;

    /**
     * 是否注册 Aspose PDF 转图片转换器。
     */
    private boolean asposePdfToImageEnabled = true;

    /**
     * 是否注册 Aspose 图片格式转换器。
     */
    private boolean asposeImagingEnabled = true;

    /**
     * Office 安装目录，为空时由 JODConverter 自动查找。
     */
    private String officeHome;

    /**
     * Office 服务端口。
     */
    private int[] officePorts = new int[]{2001, 2002};

    /**
     * Office 单任务超时时间，单位毫秒。
     */
    private long officeTaskExecutionTimeout = 300000L;

    /**
     * 是否注册 PDF 转图片转换器。
     */
    private boolean pdfToImageEnabled = true;

    /**
     * 是否注册 TIFF 转 PDF 转换器。
     */
    private boolean tiffToPdfEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHtmlToTextEnabled() {
        return htmlToTextEnabled;
    }

    public void setHtmlToTextEnabled(boolean htmlToTextEnabled) {
        this.htmlToTextEnabled = htmlToTextEnabled;
    }

    public boolean isOfficeToPdfEnabled() {
        return officeToPdfEnabled;
    }

    public void setOfficeToPdfEnabled(boolean officeToPdfEnabled) {
        this.officeToPdfEnabled = officeToPdfEnabled;
    }

    public boolean isAsposeWordToPdfEnabled() {
        return asposeWordToPdfEnabled;
    }

    public void setAsposeWordToPdfEnabled(boolean asposeWordToPdfEnabled) {
        this.asposeWordToPdfEnabled = asposeWordToPdfEnabled;
    }

    public boolean isAsposeExcelToPdfEnabled() {
        return asposeExcelToPdfEnabled;
    }

    public void setAsposeExcelToPdfEnabled(boolean asposeExcelToPdfEnabled) {
        this.asposeExcelToPdfEnabled = asposeExcelToPdfEnabled;
    }

    public boolean isAsposeSlideToPdfEnabled() {
        return asposeSlideToPdfEnabled;
    }

    public void setAsposeSlideToPdfEnabled(boolean asposeSlideToPdfEnabled) {
        this.asposeSlideToPdfEnabled = asposeSlideToPdfEnabled;
    }

    public boolean isAsposePdfToImageEnabled() {
        return asposePdfToImageEnabled;
    }

    public void setAsposePdfToImageEnabled(boolean asposePdfToImageEnabled) {
        this.asposePdfToImageEnabled = asposePdfToImageEnabled;
    }

    public boolean isAsposeImagingEnabled() {
        return asposeImagingEnabled;
    }

    public void setAsposeImagingEnabled(boolean asposeImagingEnabled) {
        this.asposeImagingEnabled = asposeImagingEnabled;
    }

    public String getOfficeHome() {
        return officeHome;
    }

    public void setOfficeHome(String officeHome) {
        this.officeHome = officeHome;
    }

    public int[] getOfficePorts() {
        return officePorts.clone();
    }

    public void setOfficePorts(int[] officePorts) {
        this.officePorts = officePorts == null ? new int[]{2001, 2002} : officePorts.clone();
    }

    public long getOfficeTaskExecutionTimeout() {
        return officeTaskExecutionTimeout;
    }

    public void setOfficeTaskExecutionTimeout(long officeTaskExecutionTimeout) {
        this.officeTaskExecutionTimeout = officeTaskExecutionTimeout;
    }

    public boolean isPdfToImageEnabled() {
        return pdfToImageEnabled;
    }

    public void setPdfToImageEnabled(boolean pdfToImageEnabled) {
        this.pdfToImageEnabled = pdfToImageEnabled;
    }

    public boolean isTiffToPdfEnabled() {
        return tiffToPdfEnabled;
    }

    public void setTiffToPdfEnabled(boolean tiffToPdfEnabled) {
        this.tiffToPdfEnabled = tiffToPdfEnabled;
    }

}
