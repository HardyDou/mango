package io.mango.template.starter;

import io.mango.infra.tools.doc.DocumentToolService;
import io.mango.infra.tools.doc.starter.DocumentToolsAutoConfiguration;
import io.mango.template.api.enums.TemplateSourceFormat;
import io.mango.template.core.mapper.TemplateMapper;
import io.mango.template.core.render.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 模板服务自动配置。
 */
@AutoConfiguration(after = DocumentToolsAutoConfiguration.class)
@ConditionalOnClass(TemplateMapper.class)
@ConditionalOnProperty(prefix = "mango.template", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.template.core.mapper")
@ComponentScan({
    "io.mango.template.core.service",
    "io.mango.template.starter"
})
public class TemplateAutoConfiguration {

    private static final int RENDER_EXECUTOR_CORE_POOL_SIZE = 2;
    private static final int RENDER_EXECUTOR_MAX_POOL_SIZE = 8;
    private static final int RENDER_EXECUTOR_QUEUE_CAPACITY = 200;
    private static final int RENDER_EXECUTOR_AWAIT_SECONDS = 30;

    @Bean
    @ConditionalOnMissingBean
    public PlaceholderTemplateEngine placeholderTemplateEngine() {
        return new PlaceholderTemplateEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    public FreemarkerTemplateEngine freemarkerTemplateEngine() {
        return new FreemarkerTemplateEngine();
    }

    @Bean
    public TemplateRenderer textTemplateRenderer(FreemarkerTemplateEngine freemarkerEngine) {
        return new TextTemplateRenderer(freemarkerEngine);
    }

    @Bean
    public TemplateRenderer htmlTemplateRenderer(FreemarkerTemplateEngine freemarkerEngine) {
        return new HtmlTemplateRenderer(freemarkerEngine);
    }

    @Bean
    public TemplateRenderer docxTemplateRenderer(PlaceholderTemplateEngine engine) {
        return new DocxTemplateRenderer(engine);
    }

    @Bean
    public TemplateRenderer xlsxTemplateRenderer(PlaceholderTemplateEngine engine) {
        return new BinaryTemplateRenderer(engine, TemplateSourceFormat.XLSX);
    }

    @Bean
    public TemplateRenderManager templateRenderManager(List<TemplateRenderer> renderers,
                                                       DocumentToolService documentToolService) {
        return new TemplateRenderManager(renderers, documentToolService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "templateRenderExecutor")
    public Executor templateRenderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(RENDER_EXECUTOR_CORE_POOL_SIZE);
        executor.setMaxPoolSize(RENDER_EXECUTOR_MAX_POOL_SIZE);
        executor.setQueueCapacity(RENDER_EXECUTOR_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("mango-template-render-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(RENDER_EXECUTOR_AWAIT_SECONDS);
        executor.initialize();
        return executor;
    }
}
