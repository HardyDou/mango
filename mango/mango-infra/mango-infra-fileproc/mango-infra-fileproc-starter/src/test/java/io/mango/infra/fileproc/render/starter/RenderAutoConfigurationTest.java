package io.mango.infra.fileproc.render.starter;

import io.mango.infra.fileproc.aspose.starter.AsposeAutoConfiguration;
import io.mango.infra.fileproc.render.RenderApi;
import io.mango.infra.fileproc.render.command.AddPdfWatermarkCommand;
import io.mango.infra.fileproc.render.command.CompressPdfCommand;
import io.mango.infra.fileproc.render.command.CompressPdfToTargetCommand;
import io.mango.infra.fileproc.render.command.MergePdfCommand;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.service.DefaultRenderApi;
import io.mango.infra.fileproc.render.service.RenderToolException;
import io.mango.infra.fileproc.render.vo.PdfCompressionResultVO;
import io.mango.infra.fileproc.render.vo.PdfOperationResultVO;
import io.mango.infra.fileproc.render.vo.RenderFormatPairVO;
import io.mango.infra.fileproc.render.vo.RenderResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AsposeAutoConfiguration.class,
                    RenderAutoConfiguration.class));

    @Test
    void withDefaultPropertiesRegistersRenderApi() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RenderApi.class);
            assertThat(context.getBean(RenderApi.class)).isInstanceOf(DefaultRenderApi.class);
            assertThat(context.getBean(RenderApi.class).canRender(RenderFormat.HTML, RenderFormat.TEXT)).isTrue();
        });
    }

    @Test
    void whenRenderDisabledRegistersNoBeans() {
        contextRunner
                .withPropertyValues("mango.fileproc.render.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RenderApi.class));
    }

    @Test
    void whenPdfOperationsDisabledRegistersNoPdfService() {
        contextRunner
                .withPropertyValues("mango.fileproc.render.pdf-operations-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(RenderApi.class);
                    assertThat(context.getBean(RenderApi.class).canRender(RenderFormat.HTML, RenderFormat.TEXT))
                            .isTrue();
                });
    }

    @Test
    void whenAsposeDisabledAndPdfOperationsDisabledStillRegistersNonPdfRendering() {
        contextRunner
                .withPropertyValues(
                        "mango.fileproc.aspose.enabled=false",
                        "mango.fileproc.render.pdf-operations-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RenderApi.class);

                    RenderApi renderApi = context.getBean(RenderApi.class);
                    assertThat(renderApi.canRender(RenderFormat.HTML, RenderFormat.TEXT)).isTrue();

                    RenderResultVO result = renderApi.render(RenderCommand.builder()
                            .sourceFormat(RenderFormat.HTML)
                            .targetFormat(RenderFormat.TEXT)
                            .inputStream(new ByteArrayInputStream("<p>Hello&nbsp;Mango</p>"
                                    .getBytes(StandardCharsets.UTF_8)))
                            .build());
                    assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("Hello Mango");

                    assertThatThrownBy(() -> renderApi.mergePdf(new MergePdfCommand("merged.pdf", List.of(
                            new io.mango.infra.fileproc.render.vo.PdfSourceVO("a.pdf",
                                    new ByteArrayInputStream(new byte[0]))), false, false)))
                            .isInstanceOf(RenderToolException.class)
                            .hasMessage("PDF 合并能力未配置");
                });
    }

    @Test
    void userProvidedPdfServiceWinsOverAutoConfiguration() {
        contextRunner
                .withUserConfiguration(CustomPdfConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(RenderApi.class);
                    assertThat(context.getBean(RenderApi.class)).isInstanceOf(CustomRenderApi.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPdfConfiguration {

        @Bean
        RenderApi pdfOperationService() {
            return new CustomRenderApi();
        }
    }

    static final class CustomRenderApi implements RenderApi {

        @Override
        public boolean canRender(RenderFormat sourceFormat, RenderFormat targetFormat) {
            return false;
        }

        @Override
        public RenderResultVO render(RenderCommand command) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public List<String> extractVariables(RenderCommand command) {
            return List.of();
        }

        @Override
        public Set<RenderFormatPairVO> supportedRenderings() {
            return Set.of();
        }

        @Override
        public PdfOperationResultVO mergePdf(MergePdfCommand command) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public PdfOperationResultVO addPdfWatermark(AddPdfWatermarkCommand command) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public PdfOperationResultVO compressPdf(CompressPdfCommand command) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public PdfCompressionResultVO compressPdfToTarget(CompressPdfToTargetCommand command) {
            throw new UnsupportedOperationException("not used");
        }
    }

    @Test
    void autoConfiguredRenderApiRendersHtmlToText() {
        contextRunner.run(context -> {
            RenderResultVO result = context.getBean(RenderApi.class).render(RenderCommand.builder()
                    .sourceFormat(RenderFormat.HTML)
                    .targetFormat(RenderFormat.TEXT)
                    .fileName("demo.html")
                    .inputStream(new ByteArrayInputStream("<p>Hello&nbsp;Mango</p>".getBytes(StandardCharsets.UTF_8)))
                    .build());

            assertThat(result.fileName()).isEqualTo("demo.txt");
            assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("Hello Mango");
        });
    }
}
