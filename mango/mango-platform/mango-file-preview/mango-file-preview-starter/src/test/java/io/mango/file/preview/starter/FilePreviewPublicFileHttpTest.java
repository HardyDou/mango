package io.mango.file.preview.starter;

import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.task.IFilePreviewTaskService;
import io.mango.file.preview.starter.controller.FilePreviewPageController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 真实 HTTP 链路：浏览器不带 {@code Authorization} 头时，预览页面里的图片、视频、表格产物
 * 都必须能直接取到，并且 Content-Type 正确。
 *
 * <p>引擎落盘文件统一挂在单一前缀 {@code /preview-file/**} 下；表格在 fileId 直取通道回落为
 * PDF 转换，页面内子资源是令牌保护的 {@code /file-preview/files/preview-artifact}。</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = FilePreviewPublicFileHttpTest.TestApplication.class,
        properties = {
            "trust.host=",
            "cache.type=jdk",
            "server.error.whitelabel.enabled=false"
        })
class FilePreviewPublicFileHttpTest {

    private static final byte[] PNG_BYTES = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8AAAwAB/AL+2wUAAAAASUVORK5CYII=");
    private static final byte[] MP4_BYTES = "00000018667479706d703432".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PDF_BYTES = "%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);

    private static final IFilePreviewService FILE_PREVIEW_SERVICE = mock(IFilePreviewService.class);
    private static final IFilePreviewTaskService PREVIEW_TASK_SERVICE = mock(IFilePreviewTaskService.class);

    private static Path fileDir;
    private static String originalFileDir;

    /**
     * 引擎的资源映射在 WebConfig 构造时按 {@link cn.keking.config.ConfigConstants#getFileDir()} 固化，
     * 因此落盘目录必须在本类的 Spring 上下文启动之前完成切换。
     */
    @BeforeAll
    static void switchEngineFileDir() throws IOException {
        fileDir = Files.createTempDirectory("mango-preview-public-");
        Files.write(fileDir.resolve("demo.png"), PNG_BYTES);
        Files.write(fileDir.resolve("file-1-abc.mp4"), MP4_BYTES);
        originalFileDir = cn.keking.config.ConfigConstants.getFileDir();
        cn.keking.config.ConfigConstants.setFileDirValue(fileDir.toString());
    }

    @AfterAll
    static void restoreEngineFileDir() throws IOException {
        cn.keking.config.ConfigConstants.setFileDirValue(originalFileDir);
        if (fileDir != null && Files.exists(fileDir)) {
            try (Stream<Path> paths = Files.walk(fileDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    @LocalServerPort
    private int port;

    @Test
    void anonymousImageRequestUnderPublicPrefixReturnsImage() {
        HttpResult response = get(FilePreviewPermitPathBeanPostProcessor.PUBLIC_FILE_PATH_PREFIX + "/demo.png");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).startsWith("image/");
        assertThat(response.body()).isEqualTo(PNG_BYTES);
    }

    @Test
    void anonymousVideoRequestUnderPublicPrefixReturnsMp4() {
        HttpResult response = get(FilePreviewPermitPathBeanPostProcessor.PUBLIC_FILE_PATH_PREFIX + "/file-1-abc.mp4");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("video/mp4");
        assertThat(response.body()).isEqualTo(MP4_BYTES);
    }

    @Test
    void anonymousSpreadsheetSubresourceServesPdfArtifact() {
        when(FILE_PREVIEW_SERVICE.downloadPreviewArtifact("ticket-sheet-sheet.xlsx"))
                .thenReturn(new FileDownloadVO(
                        new ByteArrayInputStream(PDF_BYTES), "sheet.xlsx.pdf", "application/pdf", PDF_BYTES.length));

        HttpResult response = get("/file-preview/files/preview-artifact?token=ticket-sheet-sheet.xlsx");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.body()).isEqualTo(PDF_BYTES);
    }

    private HttpResult get(String path) {
        try {
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                                    .GET()
                                    .timeout(Duration.ofSeconds(10))
                                    .build(),
                            HttpResponse.BodyHandlers.ofByteArray());
            return new HttpResult(
                    response.statusCode(),
                    response.headers().firstValue("Content-Type").orElse(""),
                    response.body());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private record HttpResult(int status, String contentType, byte[] body) {
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
        EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
        ServletWebServerFactoryAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class
    })
    static class TestApplication {

        /**
         * 引擎静态资源映射：{@code /preview-file/**} 指向引擎落盘目录。
         */
        @Bean
        cn.keking.config.WebConfig engineWebConfig() {
            return new cn.keking.config.WebConfig();
        }

        @Bean
        IFilePreviewService filePreviewService() {
            return FILE_PREVIEW_SERVICE;
        }

        @Bean
        IFilePreviewTaskService previewTaskService() {
            return PREVIEW_TASK_SERVICE;
        }

        @Bean
        FilePreviewPageController filePreviewPageController(
                IFilePreviewService filePreviewService, IFilePreviewTaskService previewTaskService) {
            return new FilePreviewPageController(filePreviewService, previewTaskService);
        }
    }
}
