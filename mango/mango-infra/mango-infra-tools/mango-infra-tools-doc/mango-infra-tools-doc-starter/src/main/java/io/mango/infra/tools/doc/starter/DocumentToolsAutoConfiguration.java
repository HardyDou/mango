package io.mango.infra.tools.doc.starter;

import io.mango.infra.tools.doc.DefaultDocumentToolService;
import io.mango.infra.tools.doc.DocumentConverter;
import io.mango.infra.tools.doc.DocumentToolRegistry;
import io.mango.infra.tools.doc.DocumentToolService;
import io.mango.infra.tools.doc.HtmlToTextDocumentConverter;
import io.mango.infra.tools.doc.PdfOperationService;
import io.mango.infra.tools.doc.SameFormatDocumentConverter;
import io.mango.infra.tools.doc.UnsupportedPdfOperationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(DocumentToolsProperties.class)
@ConditionalOnProperty(prefix = "mango.tools.doc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentToolsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SameFormatDocumentConverter.class)
    public SameFormatDocumentConverter sameFormatDocumentConverter() {
        return new SameFormatDocumentConverter();
    }

    @Bean
    @ConditionalOnMissingBean(HtmlToTextDocumentConverter.class)
    @ConditionalOnProperty(prefix = "mango.tools.doc", name = "html-to-text-enabled",
            havingValue = "true", matchIfMissing = true)
    public HtmlToTextDocumentConverter htmlToTextDocumentConverter() {
        return new HtmlToTextDocumentConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentToolRegistry documentToolRegistry(List<DocumentConverter> converters) {
        return new DocumentToolRegistry(converters);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentToolService documentToolService(DocumentToolRegistry registry) {
        return new DefaultDocumentToolService(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.tools.doc", name = "pdf-operations-enabled",
            havingValue = "true", matchIfMissing = true)
    public PdfOperationService pdfOperationService() {
        return new UnsupportedPdfOperationService();
    }
}
