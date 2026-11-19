package io.mango.file.preview.starter;

import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真实 HTTP 链路：随机端口起内嵌 Servlet 容器，请求 /onlinePreview?fileId=...，
 * 源站地址被指到不可达端口，验证预览在「对外域名不可用」时依然成立。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = FilePreviewRealHttpTest.TestApplication.class,
        properties = {
            "mango.file-preview.enabled=true",
            "mango.file-preview.source-base-url=http://127.0.0.1:1",
            "mango.file-preview.source-path=/file-preview/sources",
            "trust.host=",
            "cache.type=jdk",
            "server.error.whitelabel.enabled=false",
            "logging.level.org.springframework.web.servlet.DispatcherServlet=DEBUG"
        })
class FilePreviewRealHttpTest {

    private static final IFilePreviewService FILE_PREVIEW_SERVICE = mock(IFilePreviewService.class);

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        cn.keking.config.ConfigConstants.setKeyValue("false");
        cn.keking.config.ConfigConstants.setCacheEnabledValueValue(Boolean.FALSE);
        cn.keking.config.ConfigConstants.setTrustHostValue("default");
        cn.keking.config.ConfigConstants.setProhibitValue(new String[] {"exe", "dll"});
    }

    @AfterEach
    void tearDown() {
        cn.keking.config.ConfigConstants.setTrustHostValue("default");
        cn.keking.config.ConfigConstants.setKeyValue("false");
    }

    private HttpResult get(String path) {
        try {
            java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient().send(
                    java.net.http.HttpRequest.newBuilder(
                                    java.net.URI.create("http://127.0.0.1:" + port + path))
                            .GET()
                            .timeout(java.time.Duration.ofSeconds(10))
                            .build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record HttpResult(int status, String body) {
    }

    @Test
    void onlinePreview_fileId在对外域名不可达时仍能返回预览内容() {
        when(FILE_PREVIEW_SERVICE.openSource("ticket-real-1")).thenReturn(new FilePreviewSource(
                new ByteArrayInputStream("real http path works".getBytes(StandardCharsets.UTF_8)),
                "demo.txt", "text/plain", 21L));

        HttpResult response = get("/onlinePreview?fileId=ticket-real-1&fullfilename=demo.txt");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).startsWith("view=txt");
        assertThat(response.body()).doesNotContain("预览令牌无效或已过期");
        String textData = response.body().lines()
                .filter(line -> line.startsWith("textData="))
                .map(line -> line.substring("textData=".length()))
                .findFirst()
                .orElseThrow();
        assertThat(new String(java.util.Base64.getDecoder().decode(textData), StandardCharsets.UTF_8))
                .contains("real http path works");
    }

    @Test
    void onlinePreview_伪造票据在真实链路上也不返回内容() {
        when(FILE_PREVIEW_SERVICE.openSource("forged-real"))
                .thenThrow(new IllegalStateException("预览令牌无效或已过期"));

        HttpResult response = get("/onlinePreview?fileId=forged-real&fullfilename=demo.txt");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).contains("预览令牌无效或已过期");
        assertThat(response.body()).doesNotContain("textData=");
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
        org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration.class,
        org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration.class,
        org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        IFilePreviewService filePreviewService() {
            return FILE_PREVIEW_SERVICE;
        }

        @Bean
        cn.keking.service.cache.CacheService cacheService() {
            cn.keking.service.cache.impl.CacheServiceJDKImpl cacheService = new cn.keking.service.cache.impl.CacheServiceJDKImpl();
            cacheService.initCache();
            return cacheService;
        }

        @Bean
        cn.keking.service.FileHandlerService fileHandlerService(cn.keking.service.cache.CacheService cacheService) {
            cn.keking.service.FileHandlerService handlerService = new cn.keking.service.FileHandlerService(cacheService);
            org.springframework.test.util.ReflectionTestUtils.setField(handlerService, "uriEncoding", "UTF-8");
            return handlerService;
        }

        @Bean
        cn.keking.service.impl.OtherFilePreviewImpl otherFilePreview() {
            return new cn.keking.service.impl.OtherFilePreviewImpl();
        }

        @Bean
        cn.keking.service.impl.SimTextFilePreviewImpl simTextFilePreview(
                cn.keking.service.FileHandlerService fileHandlerService,
                cn.keking.service.impl.OtherFilePreviewImpl otherFilePreview) {
            return new cn.keking.service.impl.SimTextFilePreviewImpl(fileHandlerService, otherFilePreview);
        }

        @Bean
        cn.keking.service.FilePreviewFactory filePreviewFactory(
                org.springframework.context.ApplicationContext context,
                cn.keking.service.impl.SimTextFilePreviewImpl simTextFilePreview) {
            return new cn.keking.service.FilePreviewFactory(context) {
                @Override
                public cn.keking.service.FilePreview get(cn.keking.model.FileAttribute fileAttribute) {
                    return simTextFilePreview;
                }
            };
        }

        @Bean
        FilePreviewEngineDownloader filePreviewEngineDownloader(IFilePreviewService filePreviewService) {
            return new FilePreviewEngineDownloader(filePreviewService);
        }

        @Bean
        cn.keking.web.controller.OnlinePreviewController onlinePreviewController(
                cn.keking.service.FilePreviewFactory filePreviewFactory,
                cn.keking.service.FileHandlerService fileHandlerService,
                cn.keking.service.cache.CacheService cacheService,
                cn.keking.service.impl.OtherFilePreviewImpl otherFilePreview) {
            return new cn.keking.web.controller.OnlinePreviewController(
                    filePreviewFactory, fileHandlerService, cacheService, otherFilePreview);
        }

        /**
         * 真实 HTTP 链路只需要验证「引擎取到源文件并进入模板渲染」，
         * 模板本身不在本次改动范围内，这里用最简单的视图把模型回显成文本。
         */
        @Bean
        org.springframework.web.servlet.ViewResolver previewViewResolver() {
            return (viewName, locale) -> new org.springframework.web.servlet.View() {
                @Override
                public String getContentType() {
                    return "text/plain;charset=UTF-8";
                }

                @Override
                public void render(java.util.Map<String, ?> model,
                                   jakarta.servlet.http.HttpServletRequest request,
                                   jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
                    StringBuilder builder = new StringBuilder("view=").append(viewName);
                    model.forEach((key, value) -> builder.append("\n").append(key).append("=").append(value));
                    response.setContentType(getContentType());
                    response.getWriter().write(builder.toString());
                }
            };
        }
    }
}
