package io.mango.infra.tools.doc.starter;

import io.mango.infra.tools.doc.DocumentFormat;
import io.mango.infra.tools.doc.DocumentToolService;
import io.mango.infra.tools.doc.HtmlToTextDocumentConverter;
import io.mango.infra.tools.doc.PdfOperationService;
import io.mango.infra.tools.doc.SameFormatDocumentConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentToolsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DocumentToolsAutoConfiguration.class));

    @Test
    void documentTools_withDefaultProperties_registersCoreBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SameFormatDocumentConverter.class);
            assertThat(context).hasSingleBean(HtmlToTextDocumentConverter.class);
            assertThat(context).hasSingleBean(DocumentToolService.class);
            assertThat(context).hasSingleBean(PdfOperationService.class);

            DocumentToolService service = context.getBean(DocumentToolService.class);
            assertThat(service.canConvert(DocumentFormat.HTML, DocumentFormat.TEXT)).isTrue();
            assertThat(service.canConvert(DocumentFormat.TEXT, DocumentFormat.TEXT)).isTrue();
        });
    }

    @Test
    void documentTools_whenDisabled_registersNoToolBeans() {
        contextRunner
                .withPropertyValues("mango.tools.doc.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DocumentToolService.class);
                    assertThat(context).doesNotHaveBean(PdfOperationService.class);
                });
    }

    @Test
    void htmlToText_whenDisabled_isNotRegistered() {
        contextRunner
                .withPropertyValues("mango.tools.doc.html-to-text-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(HtmlToTextDocumentConverter.class);
                    assertThat(context).hasSingleBean(DocumentToolService.class);

                    DocumentToolService service = context.getBean(DocumentToolService.class);
                    assertThat(service.canConvert(DocumentFormat.HTML, DocumentFormat.TEXT)).isFalse();
                });
    }
}
