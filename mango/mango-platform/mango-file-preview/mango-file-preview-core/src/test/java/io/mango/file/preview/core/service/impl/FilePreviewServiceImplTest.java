package io.mango.file.preview.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.gateway.FilePreviewFileGateway;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ITokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePreviewServiceImplTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void createPreview_生成带上下文的短期入口令牌() {
        FilePreviewServiceImpl service = service();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());
        var refreshedEnginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());

        assertThat(preview.getPreviewUrl()).startsWith("/file-preview/files/preview-entry?token=");
        assertThat(enginePreview.getPreviewUrl()).startsWith("/onlinePreview?url=");
        assertThat(refreshedEnginePreview.getPreviewUrl()).startsWith("/onlinePreview?url=");
    }

    @Test
    void createEnginePreview_原始中文文件名使用FileId安全名() {
        FilePreviewProperties properties = new FilePreviewProperties();
        FilePreviewServiceImpl service = service(properties, new StubFileApi("中文 (1).DOCX"));

        String sourceUrl = sourceUrl(service.createEnginePreview(100L).getPreviewUrl());

        assertThat(sourceUrl).doesNotContain("中文");
        assertThat(queryParameter(sourceUrl, "fullfilename")).isEqualTo("file-100.docx");
    }

    @Test
    void createEnginePreview_配置内部源地址时不使用入口请求地址() {
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("http://mango-app:8080/internal/");
        FilePreviewServiceImpl service = service(properties, new StubFileApi());

        String sourceUrl = sourceUrl(service.createEnginePreview(100L).getPreviewUrl());

        assertThat(sourceUrl).startsWith("http://mango-app:8080/internal/file-preview/sources?");
    }

    @Test
    void createEnginePreview_未配置内部源地址时保持兼容回退() {
        FilePreviewServiceImpl service = service();

        String sourceUrl = sourceUrl(service.createEnginePreview(100L).getPreviewUrl());

        assertThat(sourceUrl).startsWith("http://127.0.0.1/file-preview/sources?");
    }

    @Test
    void openSource_使用服务内下载读取源文件() {
        FilePreviewServiceImpl service = service();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());
        String sourceToken = queryParameter(sourceUrl(enginePreview.getPreviewUrl()), "token");

        var source = service.openSource(sourceToken);

        assertThat(source.fileName()).isEqualTo("demo.pptx");
        assertThat(source.contentLength()).isEqualTo(1L);
    }

    @Test
    void openSource_无效令牌失败且不改变调用方上下文() {
        FilePreviewServiceImpl service = service();
        MangoContextSnapshot caller = MangoContextSnapshot.empty().withTenantId("caller");
        MangoContextHolder.set(caller);

        assertThatThrownBy(() -> service.openSource("missing"))
                .hasMessageContaining("预览令牌无效或已过期");
        assertThat(MangoContextHolder.get()).isEqualTo(caller);
    }

    @Test
    void openSource_有效令牌使用签发上下文并恢复调用方上下文() {
        StubFileContentProvider contentProvider = new StubFileContentProvider();
        FilePreviewServiceImpl service = service(new StubTokenStore(), Clock.systemUTC(), contentProvider);
        MangoContextSnapshot issuer = MangoContextSnapshot.empty().withTenantId("issuer");
        MangoContextHolder.set(issuer);
        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());
        String sourceToken = queryParameter(sourceUrl(enginePreview.getPreviewUrl()), "token");
        MangoContextSnapshot caller = MangoContextSnapshot.empty().withTenantId("caller");
        MangoContextHolder.set(caller);

        service.openSource(sourceToken);

        assertThat(contentProvider.observedContext.tenantId()).isEqualTo("issuer");
        assertThat(MangoContextHolder.get()).isEqualTo(caller);
    }

    @Test
    void validateGeneratedAccess_只允许令牌对应文件的转换结果() {
        FilePreviewServiceImpl service = service();
        var enginePreview = service.createEnginePreview(100L);
        String sourceToken = queryParameter(sourceUrl(enginePreview.getPreviewUrl()), "token");

        service.validateGeneratedAccess(sourceToken, "file-100pptx.pdf");

        assertThatThrownBy(() -> service.validateGeneratedAccess(sourceToken, "file-101pptx.pdf"))
                .hasMessageContaining("预览令牌无效或已过期");
        assertThatThrownBy(() -> service.validateGeneratedAccess(sourceToken, "../file-100pptx.pdf"))
                .hasMessageContaining("预览令牌无效或已过期");
    }

    @Test
    void createEnginePreviewByToken_令牌内容损坏时返回稳定业务错误() {
        StubTokenStore tokenStore = new StubTokenStore();
        tokenStore.values.put("file-preview:entry:broken", "not-json");
        FilePreviewServiceImpl service = service(tokenStore, Clock.systemUTC());

        assertThatThrownBy(() -> service.createEnginePreviewByToken("broken"))
                .hasMessageContaining("预览令牌无效或已过期");
    }

    @Test
    void createEnginePreviewByToken_过期后删除令牌() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T00:00:00Z"));
        StubTokenStore tokenStore = new StubTokenStore();
        FilePreviewServiceImpl service = service(tokenStore, clock);
        String token = service.createPreview(100L).getPreviewToken();
        clock.advanceSeconds(86_401);

        assertThatThrownBy(() -> service.createEnginePreviewByToken(token))
                .hasMessageContaining("预览令牌无效或已过期");
        assertThat(tokenStore.values).doesNotContainKey("file-preview:entry:" + token);
    }

    private FilePreviewServiceImpl service() {
        return service(new StubTokenStore(), Clock.systemUTC());
    }

    private FilePreviewServiceImpl service(StubTokenStore tokenStore, Clock clock) {
        return service(tokenStore, clock, new StubFileContentProvider());
    }

    private FilePreviewServiceImpl service(StubTokenStore tokenStore,
                                           Clock clock,
                                           StubFileContentProvider contentProvider) {
        return service(new FilePreviewProperties(), new StubFileApi(), tokenStore, clock, contentProvider);
    }

    private FilePreviewServiceImpl service(FilePreviewProperties properties, FileApi fileApi) {
        return service(properties, fileApi, new StubTokenStore(), Clock.systemUTC(), new StubFileContentProvider());
    }

    private FilePreviewServiceImpl service(FilePreviewProperties properties,
                                           FileApi fileApi,
                                           StubTokenStore tokenStore,
                                           Clock clock,
                                           StubFileContentProvider contentProvider) {
        FilePreviewFileGateway gateway = new FilePreviewFileGateway(fileApi, contentProvider);
        return new FilePreviewServiceImpl(gateway, properties, tokenStore, new ObjectMapper(), clock);
    }

    private String sourceUrl(String enginePreviewUrl) {
        String encodedSourceUrl = enginePreviewUrl.substring(enginePreviewUrl.indexOf("url=") + 4);
        String base64SourceUrl = java.net.URLDecoder.decode(encodedSourceUrl, java.nio.charset.StandardCharsets.UTF_8);
        return new String(Base64.getDecoder().decode(base64SourceUrl), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String queryParameter(String url, String name) {
        return org.springframework.web.util.UriComponentsBuilder.fromUriString(url)
                .build()
                .getQueryParams()
                .getFirst(name);
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static class StubTokenStore implements ITokenStore {

        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public void store(String token, String value, long ttlSeconds) {
            values.put(token, value);
        }

        @Override
        public String get(String token) {
            return values.get(token);
        }

        @Override
        public void remove(String token) {
            values.remove(token);
        }
    }

    private static class StubFileApi implements FileApi {

        private final String fileName;

        private StubFileApi() {
            this("demo.pptx");
        }

        private StubFileApi(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public R<io.mango.common.vo.PageResult<FileRecordVO>> page(io.mango.file.api.query.FileRecordPageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FileRecordVO> get(Long id) {
            FileRecordVO vo = new FileRecordVO();
            vo.setId(id);
            vo.setFileName(fileName);
            return R.ok(vo);
        }

        @Override
        public R<io.mango.file.api.vo.FilePreviewVO> preview(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FileRecordVO> packageFiles(io.mango.file.api.command.FilePackageCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<io.mango.file.api.vo.FilePackageResultVO> packageFilesWithSizeControl(
                io.mango.file.api.command.FilePackageSizeControlCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FileRecordVO> mergeToPdf(io.mango.file.api.command.FileMergePdfCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<Boolean> archive(Long id, String reason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<Boolean> delete(io.mango.file.api.command.FileDeleteCommand command) {
            throw new UnsupportedOperationException();
        }
    }

    private static class StubFileContentProvider implements IFileContentProvider {

        private MangoContextSnapshot observedContext;

        @Override
        public FileRecordVO save(SaveFileCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileDownloadVO download(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileDownloadVO downloadForService(Long id) {
            observedContext = MangoContextHolder.get();
            return new FileDownloadVO(new ByteArrayInputStream(new byte[]{1}), "demo.pptx",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation", 1L);
        }
    }
}
