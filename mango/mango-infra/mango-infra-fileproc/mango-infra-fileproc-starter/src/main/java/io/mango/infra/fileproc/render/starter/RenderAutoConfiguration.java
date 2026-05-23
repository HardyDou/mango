package io.mango.infra.fileproc.render.starter;

import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.render.RenderApi;
import io.mango.infra.fileproc.render.service.AsposePdfRenderApi;
import io.mango.infra.fileproc.render.service.DefaultRenderApi;
import io.mango.infra.fileproc.render.service.DocxRenderProvider;
import io.mango.infra.fileproc.render.service.FreemarkerRenderEngine;
import io.mango.infra.fileproc.render.service.HtmlRenderProvider;
import io.mango.infra.fileproc.render.service.HtmlToTextRenderProvider;
import io.mango.infra.fileproc.render.service.IRenderProvider;
import io.mango.infra.fileproc.render.service.OoxmlRenderProvider;
import io.mango.infra.fileproc.render.service.PlaceholderRenderEngine;
import io.mango.infra.fileproc.render.service.RenderRegistry;
import io.mango.infra.fileproc.render.service.SameFormatRenderProvider;
import io.mango.infra.fileproc.render.service.TextRenderProvider;
import io.mango.infra.fileproc.render.service.UnsupportedRenderService;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 渲染处理工具自动配置。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mango.fileproc.render", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RenderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SameFormatRenderProvider sameFormatRenderProvider() {
        return new SameFormatRenderProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public HtmlToTextRenderProvider htmlToTextRenderProvider() {
        return new HtmlToTextRenderProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public FreemarkerRenderEngine freemarkerRenderEngine() {
        return new FreemarkerRenderEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    public PlaceholderRenderEngine placeholderRenderEngine() {
        return new PlaceholderRenderEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    public TextRenderProvider textRenderProvider(FreemarkerRenderEngine freemarkerEngine) {
        return new TextRenderProvider(freemarkerEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public HtmlRenderProvider htmlRenderProvider(FreemarkerRenderEngine freemarkerEngine) {
        return new HtmlRenderProvider(freemarkerEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocxRenderProvider docxRenderProvider(PlaceholderRenderEngine placeholderEngine) {
        return new DocxRenderProvider(placeholderEngine);
    }

    @Bean
    @ConditionalOnMissingBean(name = "xlsxRenderProvider")
    public OoxmlRenderProvider xlsxRenderProvider(PlaceholderRenderEngine placeholderEngine) {
        return new OoxmlRenderProvider(placeholderEngine, RenderFormat.XLSX);
    }

    @Bean
    @ConditionalOnMissingBean
    public RenderRegistry renderRegistry(List<IRenderProvider> providers) {
        return new RenderRegistry(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public RenderApi renderApi(RenderRegistry registry,
            AsposeLicenseApi licenseApi,
            @Value("${mango.fileproc.render.pdf-operations-enabled:true}") boolean pdfOperationsEnabled) {
        return new DefaultRenderApi(registry, pdfRenderApi(licenseApi, pdfOperationsEnabled));
    }

    private RenderApi pdfRenderApi(AsposeLicenseApi licenseApi, boolean pdfOperationsEnabled) {
        if (!pdfOperationsEnabled) {
            return new UnsupportedRenderService();
        }
        return new AsposePdfRenderApi(licenseApi);
    }
}
