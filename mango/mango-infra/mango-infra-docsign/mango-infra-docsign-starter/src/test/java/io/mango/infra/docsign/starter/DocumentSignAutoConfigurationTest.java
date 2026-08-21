package io.mango.infra.docsign.starter;

import io.mango.infra.docsign.DocumentSignApi;
import io.mango.infra.docsign.command.DocumentSignCommand;
import io.mango.infra.docsign.command.DocumentVerifyCommand;
import io.mango.infra.docsign.core.OfdDocumentSignProvider;
import io.mango.infra.docsign.core.PdfDocumentSignProvider;
import io.mango.infra.docsign.enums.DocumentSignFormat;
import io.mango.infra.docsign.vo.DocumentSignResultVO;
import io.mango.infra.docsign.vo.DocumentSignStreamResultVO;
import io.mango.infra.docsign.vo.DocumentVerifyResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentSignAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DocumentSignAutoConfiguration.class));

    @Test
    void defaultProperties_registerBothProvidersAndApi() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PdfDocumentSignProvider.class);
            assertThat(context).hasSingleBean(OfdDocumentSignProvider.class);
            assertThat(context).hasSingleBean(DocumentSignApi.class);
            assertThat(context.getBean(DocumentSignApi.class).supportedFormats())
                    .containsExactlyInAnyOrder(DocumentSignFormat.PDF, DocumentSignFormat.OFD);
        });
    }

    @Test
    void disabled_registersNoDocumentSignBeans() {
        contextRunner
                .withPropertyValues("mango.docsign.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PdfDocumentSignProvider.class);
                    assertThat(context).doesNotHaveBean(OfdDocumentSignProvider.class);
                    assertThat(context).doesNotHaveBean(DocumentSignApi.class);
                });
    }

    @Test
    void pdfDisabled_registersOnlyOfdProvider() {
        contextRunner
                .withPropertyValues("mango.docsign.pdf-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PdfDocumentSignProvider.class);
                    assertThat(context).hasSingleBean(OfdDocumentSignProvider.class);
                    assertThat(context.getBean(DocumentSignApi.class).supportedFormats())
                            .containsExactly(DocumentSignFormat.OFD);
                });
    }

    @Test
    void ofdDisabled_registersOnlyPdfProvider() {
        contextRunner
                .withPropertyValues("mango.docsign.ofd-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(PdfDocumentSignProvider.class);
                    assertThat(context).doesNotHaveBean(OfdDocumentSignProvider.class);
                    assertThat(context.getBean(DocumentSignApi.class).supportedFormats())
                            .containsExactly(DocumentSignFormat.PDF);
                });
    }

    @Test
    void bothProvidersDisabled_registersNoApiAndDoesNotFailContext() {
        contextRunner
                .withPropertyValues(
                        "mango.docsign.pdf-enabled=false",
                        "mango.docsign.ofd-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DocumentSignApi.class);
                });
    }

    @Test
    void userProvidedApi_winsOverAutoConfiguration() {
        contextRunner
                .withUserConfiguration(CustomApiConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DocumentSignApi.class);
                    assertThat(context.getBean(DocumentSignApi.class)).isSameAs(CustomApi.INSTANCE);
                });
    }

    @Test
    void userProvidedProvider_preventsDuplicateConcreteProvider() {
        contextRunner
                .withUserConfiguration(CustomProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OfdDocumentSignProvider.class);
                    assertThat(context.getBean(OfdDocumentSignProvider.class))
                            .isSameAs(CustomProviderConfiguration.CUSTOM_OFD_PROVIDER);
                    assertThat(context).hasSingleBean(PdfDocumentSignProvider.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomApiConfiguration {

        @Bean
        DocumentSignApi customDocumentSignApi() {
            return CustomApi.INSTANCE;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomProviderConfiguration {

        private static final OfdDocumentSignProvider CUSTOM_OFD_PROVIDER =
                new OfdDocumentSignProvider();

        @Bean
        OfdDocumentSignProvider customOfdDocumentSignProvider() {
            return CUSTOM_OFD_PROVIDER;
        }
    }

    private enum CustomApi implements DocumentSignApi {
        INSTANCE;

        @Override
        public DocumentSignResultVO sign(DocumentSignCommand command) {
            throw new UnsupportedOperationException("test only");
        }

        @Override
        public DocumentSignStreamResultVO sign(DocumentSignCommand command,
                                               InputStream document,
                                               OutputStream signedDocument) {
            throw new UnsupportedOperationException("test only");
        }

        @Override
        public DocumentVerifyResultVO verify(DocumentVerifyCommand command) {
            throw new UnsupportedOperationException("test only");
        }

        @Override
        public DocumentVerifyResultVO verify(DocumentVerifyCommand command, InputStream document) {
            throw new UnsupportedOperationException("test only");
        }

        @Override
        public Set<DocumentSignFormat> supportedFormats() {
            return Set.of();
        }
    }
}
