package io.mango.infra.docsign.starter;

import io.mango.infra.docsign.DocumentSignApi;
import io.mango.infra.docsign.core.DefaultDocumentSignApi;
import io.mango.infra.docsign.core.OfdDocumentSignProvider;
import io.mango.infra.docsign.core.PdfDocumentSignProvider;
import io.mango.infra.docsign.spi.IDocumentSignProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Spring Boot assembly for PDF/OFD document signing providers.
 */
@AutoConfiguration
@EnableConfigurationProperties(DocumentSignProperties.class)
@ConditionalOnProperty(prefix = "mango.docsign", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class DocumentSignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.docsign", name = "pdf-enabled",
            havingValue = "true", matchIfMissing = true)
    public PdfDocumentSignProvider pdfDocumentSignProvider(DocumentSignProperties properties) {
        return new PdfDocumentSignProvider(
                properties.getMaxInMemorySize().toBytes(),
                properties.getMaxDocumentSize().toBytes(),
                properties.getTemporaryDirectory());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mango.docsign", name = "ofd-enabled",
            havingValue = "true", matchIfMissing = true)
    public OfdDocumentSignProvider ofdDocumentSignProvider(DocumentSignProperties properties) {
        return new OfdDocumentSignProvider(
                properties.getMaxInMemorySize().toBytes(),
                properties.getMaxDocumentSize().toBytes(),
                properties.getTemporaryDirectory());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(IDocumentSignProvider.class)
    public DocumentSignApi documentSignApi(List<IDocumentSignProvider> providers) {
        return new DefaultDocumentSignApi(providers);
    }
}
