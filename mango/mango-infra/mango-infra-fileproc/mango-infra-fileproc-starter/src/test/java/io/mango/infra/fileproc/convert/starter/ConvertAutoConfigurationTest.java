package io.mango.infra.fileproc.convert.starter;

import io.mango.infra.fileproc.convert.convert.DefaultConvertApi;
import io.mango.infra.fileproc.aspose.starter.AsposeAutoConfiguration;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.convert.ConvertRegistry;
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.convert.AsposeExcelToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeImagingConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposePdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeSlideToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.AsposeWordToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.HtmlToTextConverter;
import io.mango.infra.fileproc.convert.convert.OfficeManagerHolder;
import io.mango.infra.fileproc.convert.convert.OfficeToPdfConvertProvider;
import io.mango.infra.fileproc.convert.convert.PdfToImageConvertProvider;
import io.mango.infra.fileproc.convert.convert.SameFormatConverter;
import io.mango.infra.fileproc.convert.convert.TiffToPdfConvertProvider;
import io.mango.infra.fileproc.convert.vo.ConvertFormatPairVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AsposeAutoConfiguration.class,
                    ConvertAutoConfiguration.class));

    @Test
    void convert_withDefaultProperties_registersCoreBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SameFormatConverter.class);
            assertThat(context).hasSingleBean(HtmlToTextConverter.class);
            assertThat(context).hasSingleBean(AsposeWordToPdfConvertProvider.class);
            assertThat(context).hasSingleBean(AsposeExcelToPdfConvertProvider.class);
            assertThat(context).hasSingleBean(AsposeSlideToPdfConvertProvider.class);
            assertThat(context).hasSingleBean(AsposePdfToImageConvertProvider.class);
            assertThat(context).hasSingleBean(AsposeImagingConvertProvider.class);
            assertThat(context).hasSingleBean(OfficeManagerHolder.class);
            assertThat(context).hasSingleBean(OfficeToPdfConvertProvider.class);
            assertThat(context).hasSingleBean(PdfToImageConvertProvider.class);
            assertThat(context).hasSingleBean(TiffToPdfConvertProvider.class);
            assertThat(context).hasSingleBean(ConvertApi.class);

            ConvertApi service = context.getBean(ConvertApi.class);
            assertThat(service.canConvert(ConvertFormat.HTML, ConvertFormat.TEXT)).isTrue();
            assertThat(service.canConvert(ConvertFormat.TEXT, ConvertFormat.TEXT)).isTrue();
            assertThat(service.canConvert(ConvertFormat.DOCX, ConvertFormat.PDF)).isTrue();
            assertThat(service.canConvert(ConvertFormat.PDF, ConvertFormat.PNG)).isTrue();
            assertThat(service.canConvert(ConvertFormat.TIFF, ConvertFormat.PDF)).isTrue();
        });
    }

    @Test
    void convert_withDefaultProperties_exposesFullCapabilityMatrix() {
        contextRunner.run(context -> {
            ConvertApi service = context.getBean(ConvertApi.class);

            assertThat(service.supportedConversions())
                    .containsExactlyInAnyOrder(
                            new ConvertFormatPairVO(ConvertFormat.HTML, ConvertFormat.TEXT),
                            new ConvertFormatPairVO(ConvertFormat.DOC, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.DOCX, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.XLS, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.XLSX, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.PPT, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.PPTX, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.TEXT, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.HTML, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.PDF, ConvertFormat.PNG),
                            new ConvertFormatPairVO(ConvertFormat.PDF, ConvertFormat.JPEG),
                            new ConvertFormatPairVO(ConvertFormat.PNG, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.PNG, ConvertFormat.JPEG),
                            new ConvertFormatPairVO(ConvertFormat.PNG, ConvertFormat.TIFF),
                            new ConvertFormatPairVO(ConvertFormat.JPEG, ConvertFormat.PDF),
                            new ConvertFormatPairVO(ConvertFormat.JPEG, ConvertFormat.PNG),
                            new ConvertFormatPairVO(ConvertFormat.JPEG, ConvertFormat.TIFF),
                            new ConvertFormatPairVO(ConvertFormat.TIFF, ConvertFormat.PNG),
                            new ConvertFormatPairVO(ConvertFormat.TIFF, ConvertFormat.JPEG),
                            new ConvertFormatPairVO(ConvertFormat.TIFF, ConvertFormat.PDF));

            assertThat(service.canConvert(ConvertFormat.OFD, ConvertFormat.PDF)).isFalse();
            assertThat(service.canConvert(ConvertFormat.ZIP, ConvertFormat.PDF)).isFalse();
            assertThat(service.canConvert(ConvertFormat.PDF, ConvertFormat.TIFF)).isFalse();
        });
    }

    @Test
    void convert_whenDisabled_registersNoToolBeans() {
        contextRunner
                .withPropertyValues("mango.fileproc.convert.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ConvertApi.class);
                    assertThat(context).doesNotHaveBean(SameFormatConverter.class);
                    assertThat(context).doesNotHaveBean(HtmlToTextConverter.class);
                });
    }

    @Test
    void htmlToText_whenDisabled_isNotRegistered() {
        contextRunner
                .withPropertyValues(
                        "mango.fileproc.convert.html-to-text-enabled=false",
                        "mango.fileproc.convert.office-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-word-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-excel-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-slide-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-pdf-to-image-enabled=false",
                        "mango.fileproc.convert.aspose-imaging-enabled=false",
                        "mango.fileproc.convert.pdf-to-image-enabled=false",
                        "mango.fileproc.convert.tiff-to-pdf-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(HtmlToTextConverter.class);
                    assertThat(context).hasSingleBean(ConvertApi.class);

                    ConvertApi service = context.getBean(ConvertApi.class);
                    assertThat(service.canConvert(ConvertFormat.HTML, ConvertFormat.TEXT)).isFalse();
                    assertThat(service.canConvert(ConvertFormat.TEXT, ConvertFormat.TEXT)).isTrue();
                });
    }

    @Test
    void coreConverters_whenDisabled_areNotRegistered() {
        contextRunner
                .withPropertyValues(
                        "mango.fileproc.convert.office-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-word-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-excel-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-slide-to-pdf-enabled=false",
                        "mango.fileproc.convert.aspose-pdf-to-image-enabled=false",
                        "mango.fileproc.convert.aspose-imaging-enabled=false",
                        "mango.fileproc.convert.pdf-to-image-enabled=false",
                        "mango.fileproc.convert.tiff-to-pdf-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OfficeManagerHolder.class);
                    assertThat(context).doesNotHaveBean(OfficeToPdfConvertProvider.class);
                    assertThat(context).doesNotHaveBean(PdfToImageConvertProvider.class);
                    assertThat(context).doesNotHaveBean(TiffToPdfConvertProvider.class);
                    ConvertApi service = context.getBean(ConvertApi.class);
                    assertThat(service.canConvert(ConvertFormat.DOCX, ConvertFormat.PDF)).isFalse();
                    assertThat(service.canConvert(ConvertFormat.PDF, ConvertFormat.PNG)).isFalse();
                    assertThat(service.canConvert(ConvertFormat.TIFF, ConvertFormat.PDF)).isFalse();
                });
    }

    @Test
    void convert_whenAsposeDisabled_stillRegistersNonAsposeConverters() {
        contextRunner
                .withPropertyValues("mango.fileproc.aspose.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AsposeWordToPdfConvertProvider.class);
                    assertThat(context).doesNotHaveBean(AsposeExcelToPdfConvertProvider.class);
                    assertThat(context).doesNotHaveBean(AsposeSlideToPdfConvertProvider.class);
                    assertThat(context).doesNotHaveBean(AsposePdfToImageConvertProvider.class);
                    assertThat(context).doesNotHaveBean(AsposeImagingConvertProvider.class);
                    assertThat(context).hasSingleBean(HtmlToTextConverter.class);
                    assertThat(context).hasSingleBean(OfficeToPdfConvertProvider.class);
                    assertThat(context).hasSingleBean(PdfToImageConvertProvider.class);
                    assertThat(context).hasSingleBean(TiffToPdfConvertProvider.class);
                    assertThat(context).hasSingleBean(ConvertApi.class);

                    ConvertApi service = context.getBean(ConvertApi.class);
                    assertThat(service.canConvert(ConvertFormat.HTML, ConvertFormat.TEXT)).isTrue();
                    assertThat(service.canConvert(ConvertFormat.DOCX, ConvertFormat.PDF)).isTrue();
                    assertThat(service.canConvert(ConvertFormat.PDF, ConvertFormat.PNG)).isTrue();
                    assertThat(service.canConvert(ConvertFormat.TIFF, ConvertFormat.PDF)).isTrue();
                    assertThat(service.canConvert(ConvertFormat.PNG, ConvertFormat.JPEG)).isFalse();
                });
    }

    @Test
    void userProvidedBeans_winOverAutoConfiguration() {
        contextRunner
                .withUserConfiguration(OverrideConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ConvertApi.class);
                    assertThat(context.getBean(ConvertApi.class)).isInstanceOf(CustomConvertApi.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class OverrideConfiguration {

        @Bean
        ConvertApi convertApi() {
            return new CustomConvertApi();
        }

    }

    static final class CustomConvertApi extends DefaultConvertApi {

        CustomConvertApi() {
            super(new ConvertRegistry(java.util.List.of()));
        }
    }

}
