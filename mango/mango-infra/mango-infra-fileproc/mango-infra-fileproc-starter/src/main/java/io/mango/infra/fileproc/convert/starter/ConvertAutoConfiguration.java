package io.mango.infra.fileproc.convert.starter;

import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.convert.AsposeExcelToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeImagingConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposePdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeSlideToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeWordToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.ConvertRegistry;
import io.mango.infra.fileproc.convert.convert.DefaultConvertApi;
import io.mango.infra.fileproc.convert.convert.HtmlToTextConverter;
import io.mango.infra.fileproc.convert.convert.IConvertProvider;
import io.mango.infra.fileproc.convert.convert.OfficeManagerHolder;
import io.mango.infra.fileproc.convert.convert.OfficeToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.PdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.convert.SameFormatConverter;
import io.mango.infra.fileproc.convert.convert.TiffToPdfConvertProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
    @Order(10)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-word-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeWordToPdfConvertProvider asposeWordToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeWordToPdfConvertProvider(licenseApi);
    }

    @Bean
    @Order(11)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-excel-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeExcelToPdfConvertProvider asposeExcelToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeExcelToPdfConvertProvider(licenseApi);
    }

    @Bean
    @Order(12)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-slide-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeSlideToPdfConvertProvider asposeSlideToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeSlideToPdfConvertProvider(licenseApi);
    }

    @Bean
    @Order(13)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-pdf-to-image-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposePdfToImageConvertProvider asposePdfToImageConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposePdfToImageConvertProvider(licenseApi);
    }

    @Bean
    @Order(14)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "aspose-imaging-enabled",
            havingValue = "true", matchIfMissing = true)
    public AsposeImagingConvertProvider asposeImagingConvertProvider(AsposeLicenseApi licenseApi) {
        return new AsposeImagingConvertProvider(licenseApi);
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
    @Order(100)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.fileproc.convert", name = "office-to-pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public OfficeToPdfConvertProvider officeToPdfConvertProvider(OfficeManagerHolder officeManagerHolder) {
        return new OfficeToPdfConvertProvider(officeManagerHolder);
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

    @Bean
    @ConditionalOnMissingBean
    public ConvertApi convertApi(ConvertRegistry registry) {
        return new DefaultConvertApi(registry);
    }

}
