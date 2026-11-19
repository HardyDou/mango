package io.mango.infra.fileproc.convert.starter;

import io.mango.infra.fileproc.aspose.AsposeFontAvailability;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.starter.AsposeAutoConfiguration;
import io.mango.infra.fileproc.aspose.starter.AsposeFontInitializer;
import io.mango.infra.fileproc.aspose.starter.AsposeProperties;
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.convert.AsposeExcelToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeImagingConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposePdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeSlideToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeWordToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.ConvertRegistry;
import io.mango.infra.fileproc.convert.convert.DefaultConvertApi;
import io.mango.infra.fileproc.convert.convert.DocxToOfdConvertProvider;
import io.mango.infra.fileproc.convert.convert.HtmlToTextConverter;
import io.mango.infra.fileproc.convert.convert.IConvertProvider;
import io.mango.infra.fileproc.convert.convert.ImageToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.OfficeManagerHolder;
import io.mango.infra.fileproc.convert.convert.OfficeToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.PdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.convert.PdfToOfdConvertProvider;
import io.mango.infra.fileproc.convert.convert.SameFormatConverter;
import io.mango.infra.fileproc.convert.convert.TiffToPdfConvertProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.util.List;

/**
 * 格式转换工具自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(ConvertProperties.class)
@ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConvertAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SameFormatConverter.class)
    public SameFormatConverter sameFormatConverter() {
        return new SameFormatConverter();
    }

    @Bean
    @ConditionalOnMissingBean(HtmlToTextConverter.class)
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "html-to-text-enabled",
            havingValue = "true", matchIfMissing = true)
    public HtmlToTextConverter htmlToTextConverter() {
        return new HtmlToTextConverter();
    }

    @Bean
    @Order(9)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "image-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public ImageToPdfConvertProvider imageToPdfConvertProvider() {
        return new ImageToPdfConvertProvider();
    }

    @Bean
    // 默认引擎：Aspose 的一致性与单文档耗时都优于 LibreOffice，LibreOffice 只作兜底。
    @Order(5)
    @ConditionalOnBean(AsposeLicenseApi.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-word-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeWordToPdfConvertProvider asposeWordToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeWordToPdfConvertProvider(licenseApi);
    }

    @Bean
    // 默认引擎：与 Word 一致，Excel 也先用 Aspose。
    @Order(6)
    @ConditionalOnBean(AsposeLicenseApi.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-excel-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeExcelToPdfConvertProvider asposeExcelToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeExcelToPdfConvertProvider(licenseApi);
    }

    @Bean
    // 默认引擎：与 Word 一致，PPT 也先用 Aspose。
    @Order(7)
    @ConditionalOnBean(AsposeLicenseApi.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-slide-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeSlideToPdfConvertProvider asposeSlideToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeSlideToPdfConvertProvider(licenseApi);
    }

    @Bean
    @Order(13)
    @ConditionalOnBean(AsposeLicenseApi.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-pdf-to-image-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposePdfToImageConvertProvider asposePdfToImageConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposePdfToImageConvertProvider(licenseApi);
    }

    @Bean
    @Order(14)
    @ConditionalOnBean(AsposeLicenseApi.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-imaging-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeImagingConvertProvider asposeImagingConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeImagingConvertProvider(licenseApi);
    }

    @Bean
    @Order(20)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "pdf-to-ofd-enabled",
            havingValue = "true", matchIfMissing = true)
    public PdfToOfdConvertProvider pdfToOfdConvertProvider() {
        return new PdfToOfdConvertProvider();
    }

    @Bean
    @Order(21)
    @ConditionalOnBean({AsposeWordToPdfConvertProvider.class, PdfToOfdConvertProvider.class})
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "docx-to-ofd-enabled",
            havingValue = "true", matchIfMissing = true)
    public DocxToOfdConvertProvider docxToOfdWithAsposeConvertProvider(
            AsposeWordToPdfConvertProvider wordToPdfProvider,
            PdfToOfdConvertProvider pdfToOfdProvider) {
        return new DocxToOfdConvertProvider(wordToPdfProvider, pdfToOfdProvider);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "office-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public OfficeManagerHolder officeManagerHolder(ConvertProperties properties) {
        File officeHome = null;
        if (properties.getOfficeHome() != null && !properties.getOfficeHome().isBlank()) {
            officeHome = new File(properties.getOfficeHome());
        }
        return new OfficeManagerHolder(officeHome, properties.getOfficePorts(),
                properties.getOfficeTaskExecutionTimeout());
    }

    @Bean
    // 兜底引擎：排在全部 Aspose 转换器（5-7）之后，只有关掉对应的 aspose-*-to-pdf-enabled
    // 才会被选中，用来在无 Aspose 授权或字体缺失的环境里保住基本转换能力。
    @Order(30)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "office-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public OfficeToPdfConvertProvider officeToPdfConvertProvider(OfficeManagerHolder officeManagerHolder) {
        return new OfficeToPdfConvertProvider(officeManagerHolder);
    }

    @Bean
    @Order(22)
    @ConditionalOnBean({OfficeToPdfConvertProvider.class, PdfToOfdConvertProvider.class})
    @ConditionalOnMissingBean(DocxToOfdConvertProvider.class)
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "docx-to-ofd-enabled",
            havingValue = "true", matchIfMissing = true)
    public DocxToOfdConvertProvider docxToOfdWithOfficeConvertProvider(
            OfficeToPdfConvertProvider officeToPdfProvider,
            PdfToOfdConvertProvider pdfToOfdProvider) {
        return new DocxToOfdConvertProvider(officeToPdfProvider, pdfToOfdProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "pdf-to-image-enabled",
            havingValue = "true", matchIfMissing = true)
    public PdfToImageConvertProvider pdfToImageConvertProvider() {
        return new PdfToImageConvertProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "tiff-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public TiffToPdfConvertProvider tiffToPdfConvertProvider() {
        return new TiffToPdfConvertProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConvertRegistry convertRegistry(List<IConvertProvider> providers) {
        return new ConvertRegistry(providers);
    }

    /**
     * Aspose 字体与授权启动自检。
     *
     * <p>默认引擎是 Aspose，缺字体或缺授权只会静默产出方块字或水印，因此在启动期 fail-fast。
     * 需要 {@link AsposeProperties}，所以 Aspose 自动配置关闭时本 bean 也不装配。</p>
     */
    @Bean
    @ConditionalOnBean({AsposeLicenseApi.class, AsposeFontAvailability.class, AsposeProperties.class})
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-font-check-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeFontInitializer asposeFontInitializer(ConvertProperties convertProperties,
                                                       AsposeProperties asposeProperties,
                                                       AsposeLicenseApi licenseApi,
                                                       AsposeFontAvailability fontAvailability) {
        return new AsposeFontInitializer(convertProperties, asposeProperties, licenseApi, fontAvailability);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConvertApi convertApi(ConvertRegistry registry) {
        return new DefaultConvertApi(registry);
    }

}
