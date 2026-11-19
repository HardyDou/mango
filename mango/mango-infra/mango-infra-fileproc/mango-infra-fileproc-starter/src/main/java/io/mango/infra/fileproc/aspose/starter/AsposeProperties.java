package io.mango.infra.fileproc.aspose.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Aspose 工具配置。
 */
@ConfigurationProperties(prefix = "mango.fileproc.aspose")
public class AsposeProperties {

    /**
     * 是否启用 Aspose 工具自动配置。
     */
    private boolean enabled = true;

    /**
     * Aspose License 通用位置。支持 classpath 路径、文件路径或目录路径。
     */
    private String licenseLocation = "classpath:/aspose/license.xml";

    /**
     * Aspose.Words License 位置。
     */
    private String wordsLicenseLocation;

    /**
     * Aspose.Cells License 位置。
     */
    private String cellsLicenseLocation;

    /**
     * Aspose.Slides License 位置。
     */
    private String slidesLicenseLocation;

    /**
     * Aspose.PDF License 位置。
     */
    private String pdfLicenseLocation;

    /**
     * Aspose.Imaging License 位置。
     */
    private String imagingLicenseLocation;

    /**
     * 额外字体目录，追加到 Aspose.Words 现有字体来源之后。
     *
     * <p>只追加不替换：系统字体来源仍然生效，缺失的中文字体从这些目录补齐。
     * 为空时完全不触碰 Aspose 的全局字体设置。</p>
     */
    private List<String> fontsFolders = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLicenseLocation() {
        return licenseLocation;
    }

    public void setLicenseLocation(String licenseLocation) {
        this.licenseLocation = licenseLocation;
    }

    public String getWordsLicenseLocation() {
        return wordsLicenseLocation;
    }

    public void setWordsLicenseLocation(String wordsLicenseLocation) {
        this.wordsLicenseLocation = wordsLicenseLocation;
    }

    public String getCellsLicenseLocation() {
        return cellsLicenseLocation;
    }

    public void setCellsLicenseLocation(String cellsLicenseLocation) {
        this.cellsLicenseLocation = cellsLicenseLocation;
    }

    public String getSlidesLicenseLocation() {
        return slidesLicenseLocation;
    }

    public void setSlidesLicenseLocation(String slidesLicenseLocation) {
        this.slidesLicenseLocation = slidesLicenseLocation;
    }

    public String getPdfLicenseLocation() {
        return pdfLicenseLocation;
    }

    public void setPdfLicenseLocation(String pdfLicenseLocation) {
        this.pdfLicenseLocation = pdfLicenseLocation;
    }

    public String getImagingLicenseLocation() {
        return imagingLicenseLocation;
    }

    public void setImagingLicenseLocation(String imagingLicenseLocation) {
        this.imagingLicenseLocation = imagingLicenseLocation;
    }

    public List<String> getFontsFolders() {
        return fontsFolders;
    }

    public void setFontsFolders(List<String> fontsFolders) {
        this.fontsFolders = fontsFolders;
    }

}
