package io.mango.infra.fileproc.compress.starter;

import io.mango.infra.fileproc.compress.FileCompressApi;
import io.mango.infra.fileproc.compress.service.GhostscriptPdfFileCompressProvider;
import io.mango.infra.fileproc.compress.service.ImageFileCompressProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FileCompressAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FileCompressAutoConfiguration.class));

    @Test
    void withDefaultProperties_registersCompressionBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ImageFileCompressProvider.class);
            assertThat(context).hasSingleBean(GhostscriptPdfFileCompressProvider.class);
            assertThat(context).hasSingleBean(FileCompressApi.class);
            assertThat(context.getBean(FileCompressApi.class).supports("demo.pdf", "application/pdf")).isTrue();
            assertThat(context.getBean(FileCompressApi.class).supports("demo.jpg", "image/jpeg")).isTrue();
        });
    }

    @Test
    void withGhostscriptProperties_registersConfiguredPath() {
        contextRunner
                .withPropertyValues("mango.fileproc.compress.ghostscript-path=/opt/gs/bin/gs")
                .run(context -> assertThat(context).hasSingleBean(GhostscriptPdfFileCompressProvider.class));
    }

    @Test
    void whenDisabled_registersNoCompressionBeans() {
        contextRunner
                .withPropertyValues("mango.fileproc.compress.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FileCompressApi.class);
                    assertThat(context).doesNotHaveBean(ImageFileCompressProvider.class);
                    assertThat(context).doesNotHaveBean(GhostscriptPdfFileCompressProvider.class);
                });
    }
}
