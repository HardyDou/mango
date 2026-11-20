package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.web.controller.FileController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 真实 HTTP 链路：压缩包解包目录必须落在 file-dir 之外，条目只能通过受控端点读取。
 *
 * <p>覆盖三类事实：</p>
 * <ul>
 *   <li>解包目录不再位于 file-dir，{@code /preview-file/<解包键>/<条目>} 与 {@code /<解包键>/<条目>}
 *       两条静态映射都取不到解包内容；</li>
 *   <li>{@code /directory} 返回的条目 id 可以直接用于 {@code /compressed-file}，且越界路径被拒绝；</li>
 *   <li>Issue #1369 的顶层 {@code file-*} 文件仍由 {@code /preview-file/**} 提供。</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = CompressFileExtractDirHttpTest.TestApplication.class,
        properties = {
            "trust.host=",
            "cache.type=jdk",
            "server.error.whitelabel.enabled=false"
        })
class CompressFileExtractDirHttpTest {

    private static final byte[] PNG_BYTES = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8AAAwAB/AL+2wUAAAAASUVORK5CYII=");
    private static final String ARCHIVE_NAME = "bundle.zip";
    private static final String ARCHIVE_KEY = "bundle.zip_";
    private static final String IMAGE_ENTRY = "docs/pic.png";
    private static final String TEXT_ENTRY = "docs/secret.txt";
    private static final String SECRET_CONTENT = "compress-entry-secret-1386";
    private static final String PUBLIC_FILE_NAME = "file-1369.png";

    private static Path fileDir;
    private static Path decompressDir;
    private static String originalFileDir;

    /**
     * 引擎的静态资源映射在 WebConfig 构造时按 {@link ConfigConstants#getFileDir()} 固化，解包根同理，
     * 因此两个目录都必须在本类的 Spring 上下文启动之前切好；解包本身走真实的
     * {@link CompressFileReader}，只有缓存协作者用 mock。
     */
    @BeforeAll
    static void extractArchive() throws Exception {
        fileDir = Files.createTempDirectory("mango-compress-file-dir-");
        decompressDir = Files.createTempDirectory("mango-compress-extract-dir-");
        Files.write(fileDir.resolve(PUBLIC_FILE_NAME), PNG_BYTES);
        writeZipArchive(fileDir.resolve(ARCHIVE_NAME));

        originalFileDir = ConfigConstants.getFileDir();
        ConfigConstants.setFileDirValue(fileDir.toString());
        ConfigConstants.setDecompressDirValue(decompressDir.toString());

        String compressFileKey = new CompressFileReader(mock(FileHandlerService.class))
                .unRar(fileDir.resolve(ARCHIVE_NAME).toString(), null, ARCHIVE_NAME,
                        new FileAttribute(FileType.COMPRESS, "zip", ARCHIVE_NAME, null));
        assertThat(compressFileKey).isEqualTo(ARCHIVE_KEY);
    }

    @AfterAll
    static void restoreDirectories() throws IOException {
        ConfigConstants.setFileDirValue(originalFileDir);
        ConfigConstants.setDecompressDirValue(ConfigConstants.DEFAULT_VALUE);
        deleteRecursively(fileDir);
        deleteRecursively(decompressDir);
    }

    @LocalServerPort
    private int port;

    @Test
    void extraction_landsOutsideFileDir() {
        assertThat(Files.exists(fileDir.resolve(ARCHIVE_KEY))).isFalse();
        assertThat(Files.isRegularFile(decompressDir.resolve(ARCHIVE_KEY).resolve(IMAGE_ENTRY))).isTrue();
    }

    @Test
    void previewFileMapping_whenExtractDirUnderFileDirRejected_returnsNotFound() {
        HttpResult imageResponse = get("/preview-file/" + ARCHIVE_KEY + "/" + IMAGE_ENTRY);
        HttpResult textResponse = get("/preview-file/" + ARCHIVE_KEY + "/" + TEXT_ENTRY);

        assertThat(imageResponse.status()).isEqualTo(404);
        assertThat(imageResponse.body()).isNotEqualTo(PNG_BYTES);
        assertThat(textResponse.status()).isEqualTo(404);
        assertThat(bodyText(textResponse)).doesNotContain(SECRET_CONTENT);
    }

    @Test
    void rootMapping_whenExtractDirUnderFileDirRejected_returnsNotFound() {
        HttpResult imageResponse = get("/" + ARCHIVE_KEY + "/" + IMAGE_ENTRY);
        HttpResult textResponse = get("/" + ARCHIVE_KEY + "/" + TEXT_ENTRY);

        assertThat(imageResponse.status()).isEqualTo(404);
        assertThat(imageResponse.body()).isNotEqualTo(PNG_BYTES);
        assertThat(textResponse.status()).isEqualTo(404);
        assertThat(bodyText(textResponse)).doesNotContain(SECRET_CONTENT);
    }

    @Test
    void compressedFile_whenEntryPathTraverses_rejectsWithoutContent() {
        String encodedTraversal = encode(ARCHIVE_KEY + "/../../outside/" + TEXT_ENTRY);
        String rawTraversal = ARCHIVE_KEY + "/..%2f..%2foutside%2fsecret.txt";
        String absolutePath = encode("/etc/passwd");

        HttpResult encodedResponse = get("/compressed-file?kkCompressfileKey=" + ARCHIVE_KEY
                + "&kkCompressfilepath=" + encodedTraversal + "&fullfilename=secret.txt");
        HttpResult rawResponse = get("/compressed-file?kkCompressfileKey=" + ARCHIVE_KEY
                + "&kkCompressfilepath=" + rawTraversal + "&fullfilename=secret.txt");
        HttpResult absoluteResponse = get("/compressed-file?kkCompressfileKey=" + ARCHIVE_KEY
                + "&kkCompressfilepath=" + absolutePath + "&fullfilename=passwd");

        assertThat(encodedResponse.status()).isEqualTo(400);
        assertThat(bodyText(encodedResponse)).doesNotContain(SECRET_CONTENT);
        assertThat(rawResponse.status()).isEqualTo(400);
        assertThat(bodyText(rawResponse)).doesNotContain(SECRET_CONTENT);
        assertThat(absoluteResponse.status()).isEqualTo(400);
        assertThat(bodyText(absoluteResponse)).doesNotContain("root:");
    }

    @Test
    void directoryPage_whenEntriesRequestedThroughCompressedFileEndpoint_returnsContent() {
        HttpResult directoryResponse = get("/directory?urls=" + encode(base64("http://" + ARCHIVE_KEY)));

        assertThat(directoryResponse.status()).isEqualTo(200);
        String directoryJson = bodyText(directoryResponse);
        assertThat(directoryJson).contains("\"id\":\"" + ARCHIVE_KEY + "/" + IMAGE_ENTRY + "\"");
        assertThat(directoryJson).doesNotContain("/preview-file/");

        HttpResult imageResponse = get("/compressed-file?kkCompressfileKey=" + ARCHIVE_KEY
                + "&kkCompressfilepath=" + encode(ARCHIVE_KEY + "/" + IMAGE_ENTRY) + "&fullfilename=pic.png");
        assertThat(imageResponse.status()).isEqualTo(200);
        assertThat(imageResponse.contentType()).isEqualTo("image/png");
        assertThat(imageResponse.body()).isEqualTo(PNG_BYTES);

        HttpResult textResponse = get("/compressed-file?kkCompressfileKey=" + ARCHIVE_KEY
                + "&kkCompressfilepath=" + encode(ARCHIVE_KEY + "/" + TEXT_ENTRY) + "&fullfilename=secret.txt");
        assertThat(textResponse.status()).isEqualTo(200);
        assertThat(textResponse.contentType()).isEqualTo("text/plain");
        assertThat(textResponse.body().length).isGreaterThan(0);
    }

    @Test
    void previewFileMapping_whenTopLevelFileFromIssue1369_stillServed() {
        HttpResult response = get("/preview-file/" + PUBLIC_FILE_NAME);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.body()).isEqualTo(PNG_BYTES);
    }

    private HttpResult get(String path) {
        try {
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                                    .GET()
                                    .timeout(Duration.ofSeconds(30))
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String bodyText(HttpResult result) {
        return new String(result.body(), StandardCharsets.UTF_8);
    }

    private static void writeZipArchive(Path archive) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            addEntry(zipOutputStream, IMAGE_ENTRY, PNG_BYTES);
            addEntry(zipOutputStream, TEXT_ENTRY, SECRET_CONTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void addEntry(ZipOutputStream zipOutputStream, String entryName, byte[] content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content);
        zipOutputStream.closeEntry();
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
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
         * 引擎静态资源映射：{@code /preview-file/**} 与根路径 {@code /**} 都指向 file-dir。
         */
        @Bean
        cn.keking.config.WebConfig engineWebConfig() {
            return new cn.keking.config.WebConfig();
        }

        @Bean
        CompressFileEntryService compressFileEntryService() {
            return new CompressFileEntryService();
        }

        @Bean
        FileController fileController(CompressFileEntryService compressFileEntryService) {
            return new FileController(compressFileEntryService);
        }
    }
}
