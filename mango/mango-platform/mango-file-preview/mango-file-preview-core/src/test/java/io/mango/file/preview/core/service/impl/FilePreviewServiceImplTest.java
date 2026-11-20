package io.mango.file.preview.core.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.preview.api.enums.FilePreviewCode;
import io.mango.file.preview.api.enums.FilePreviewRenderMode;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.file.preview.core.service.PreviewSourceAvailability;
import io.mango.file.preview.core.gateway.FilePreviewFileGateway;
import io.mango.file.preview.core.service.model.FilePreviewMode;
import io.mango.file.preview.core.task.IFilePreviewTaskService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ITokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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

        assertThat(preview.getPreviewUrl()).startsWith("/api/file-preview/files/preview-entry?token=");
        assertThat(enginePreview.getPreviewUrl()).startsWith("/api/onlinePreview?url=");
        assertThat(refreshedEnginePreview.getPreviewUrl()).startsWith("/api/onlinePreview?url=");
    }

    @Test
    void createEnginePreview_配置引擎对外地址时产出绝对地址而入口保持相对() {
        FilePreviewProperties properties = properties();
        properties.setEnginePublicBaseUrl("https://engine.example.com/");
        FilePreviewServiceImpl service = service(properties, new StubFileApi("demo.png"));
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());

        assertThat(preview.getPreviewUrl())
                .startsWith("/api/file-preview/files/preview-entry?token=")
                .doesNotContain("engine.example.com");
        assertThat(enginePreview.getPreviewUrl())
                .startsWith("https://engine.example.com/api/onlinePreview?");
    }

    @Test
    void createEnginePreview_未配置引擎对外地址时保持相对地址() {
        FilePreviewServiceImpl service = service(properties(), new StubFileApi("demo.png"));
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());

        assertThat(preview.getPreviewUrl()).startsWith("/api/file-preview/files/preview-entry?token=");
        assertThat(enginePreview.getPreviewUrl()).startsWith("/api/onlinePreview?");
    }

    @Test
    void createEnginePreview_原始中文文件名使用FileId安全名() {
        FilePreviewServiceImpl service = service(properties(), new StubFileApi("中文 (1).DOCX"));

        String sourceUrl = sourceUrl(service.createEnginePreview(100L).getPreviewUrl());

        assertThat(sourceUrl).doesNotContain("中文");
        assertThat(queryParameter(sourceUrl, "fullfilename")).matches("file-100-[0-9a-f]{16}\\.docx");
    }

    @Test
    void createEnginePreview_文件版本变化生成不同源文件名() {
        StubFileApi firstApi = new StubFileApi("demo.pptx", "hash-a");
        StubFileApi secondApi = new StubFileApi("demo.pptx", "hash-b");

        String first = queryParameter(sourceUrl(service(properties(), firstApi).createEnginePreview(100L).getPreviewUrl()), "fullfilename");
        String second = queryParameter(sourceUrl(service(properties(), secondApi).createEnginePreview(100L).getPreviewUrl()), "fullfilename");

        assertThat(first).isNotEqualTo(second);
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
    void createEnginePreview_有直取实现时引擎地址只带fileId参数() {
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("https://business.example.com");
        FilePreviewServiceImpl service = service(properties, new StubFileApi(), new StubTokenStore(),
                Clock.systemUTC(), new StubFileContentProvider(), () -> true);

        String previewUrl = service.createEnginePreview(100L).getPreviewUrl();

        assertThat(previewUrl).startsWith("/api/onlinePreview?fileId=");
        assertThat(previewUrl).doesNotContain("business.example.com");
        assertThat(previewUrl).doesNotContain("url=");
        assertThat(queryParameter(previewUrl, "fullfilename")).matches("file-100-[0-9a-f]{16}\\.pptx");
    }

    @Test
    void createEnginePreview_auto模式无直取实现时保持HTTP抓取() {
        FilePreviewServiceImpl service = service();

        String sourceUrl = sourceUrl(service.createEnginePreview(100L).getPreviewUrl());

        assertThat(sourceUrl).startsWith("http://127.0.0.1/file-preview/sources?");
    }

    @Test
    void createEnginePreview_缺少sourceBaseUrl时报业务错误而不是静默回退() {
        FilePreviewServiceImpl service = service(new FilePreviewProperties(), new StubFileApi());

        assertThatThrownBy(() -> service.createEnginePreview(100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("mango.file-preview.source-base-url")
                .satisfies(exception -> assertThat(((BizException) exception).getCode())
                        .isEqualTo(FilePreviewCode.SOURCE_BASE_URL_MISSING.getCode()));
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
    void openSource_二百兆文件保持流式返回() {
        StubFileContentProvider contentProvider = new LargeFileContentProvider();
        FilePreviewServiceImpl service = service(new StubTokenStore(), Clock.systemUTC(), contentProvider);
        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());
        String sourceToken = queryParameter(sourceUrl(enginePreview.getPreviewUrl()), "token");

        var source = service.openSource(sourceToken);

        assertThat(source.contentLength()).isEqualTo(200L * 1024 * 1024);
        try {
            assertThat(source.inputStream().read()).isEqualTo(1);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
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

        String generatedName = queryParameter(sourceUrl(enginePreview.getPreviewUrl()), "fullfilename").replace(".pptx", "pptx.pdf");
        service.validateGeneratedAccess(sourceToken, generatedName);

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
    void isSourceTokenValid_有效令牌为真且不消费令牌() {
        StubTokenStore tokenStore = new StubTokenStore();
        FilePreviewServiceImpl service = service(tokenStore, Clock.systemUTC());
        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());
        String sourceToken = queryParameter(sourceUrl(enginePreview.getPreviewUrl()), "token");

        assertThat(service.isSourceTokenValid(sourceToken)).isTrue();
        assertThat(tokenStore.values).containsKey("file-preview:source:" + sourceToken);
    }

    @Test
    void isSourceTokenValid_不存在或空白令牌为假() {
        FilePreviewServiceImpl service = service();

        assertThat(service.isSourceTokenValid("missing")).isFalse();
        assertThat(service.isSourceTokenValid("  ")).isFalse();
        assertThat(service.isSourceTokenValid(null)).isFalse();
    }

    @Test
    void isSourceTokenValid_过期令牌为假且不抛异常() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T00:00:00Z"));
        StubTokenStore tokenStore = new StubTokenStore();
        FilePreviewServiceImpl service = service(tokenStore, clock);
        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());
        String sourceToken = queryParameter(sourceUrl(enginePreview.getPreviewUrl()), "token");
        clock.advanceSeconds(86_401);

        assertThat(service.isSourceTokenValid(sourceToken)).isFalse();
    }

    @Test
    void isSourceTokenValid_令牌内容损坏时按不可用处理() {
        StubTokenStore tokenStore = new StubTokenStore();
        tokenStore.values.put("file-preview:source:broken", "not-json");
        FilePreviewServiceImpl service = service(tokenStore, Clock.systemUTC());

        assertThat(service.isSourceTokenValid("broken")).isFalse();
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

    @Test
    void previewMode_按扩展名选择渲染方式() {
        assertThat(previewModeOf(new StubFileApi("demo.pdf"))).isEqualTo(FilePreviewMode.NATIVE_PDF);
        assertThat(previewModeOf(new StubFileApi("demo.docx"))).isEqualTo(FilePreviewMode.PDF_CONVERSION);
        assertThat(previewModeOf(new StubFileApi("demo.odt"))).isEqualTo(FilePreviewMode.PDF_CONVERSION);
        assertThat(previewModeOf(new StubFileApi("demo.ods"))).isEqualTo(FilePreviewMode.PDF_CONVERSION);
        assertThat(previewModeOf(new StubFileApi("demo.odp"))).isEqualTo(FilePreviewMode.PDF_CONVERSION);
        assertThat(previewModeOf(new StubFileApi("demo.xlsx"))).isEqualTo(FilePreviewMode.ENGINE);
        assertThat(previewModeOf(new StubFileApi("demo.zip"))).isEqualTo(FilePreviewMode.ENGINE);
        assertThat(previewModeOf(new StubFileApi("demo.rar"))).isEqualTo(FilePreviewMode.ENGINE);
        assertThat(previewModeOf(new StubFileApi("demo.7z"))).isEqualTo(FilePreviewMode.ENGINE);
    }

    private FilePreviewMode previewModeOf(FileApi fileApi) {
        FilePreviewServiceImpl service = service(properties(), fileApi);
        return service.previewMode(service.createPreview(100L).getPreviewToken());
    }

    @Test
    void previewMode_fileId直取通道下表格回落PDF转换() {
        FilePreviewProperties properties = properties();

        assertThat(previewModeOf(new StubFileApi("sheet.xlsx"), properties, () -> true))
                .isEqualTo(FilePreviewMode.PDF_CONVERSION);
        assertThat(previewModeOf(new StubFileApi("sheet.csv"), properties, () -> true))
                .isEqualTo(FilePreviewMode.PDF_CONVERSION);
        // 表格以外的形态不变：压缩包仍走引擎。
        assertThat(previewModeOf(new StubFileApi("data.zip"), properties, () -> true))
                .isEqualTo(FilePreviewMode.ENGINE);
        // url 兜底通道（无进程内直取）仍保留引擎的表格前端渲染。
        assertThat(previewModeOf(new StubFileApi("sheet.xlsx"), properties, () -> false))
                .isEqualTo(FilePreviewMode.ENGINE);
    }

    @Test
    void renderMode_fileId直取通道下表格进入转换中() {
        assertThat(renderModeOf(new StubFileApi("sheet.xlsx"), () -> true))
                .isEqualTo(FilePreviewRenderMode.CONVERTING);
    }

    private FilePreviewMode previewModeOf(FileApi fileApi, FilePreviewProperties properties,
                                         PreviewSourceAvailability availability) {
        FilePreviewServiceImpl service = service(properties, fileApi, new StubTokenStore(), Clock.systemUTC(),
                new StubFileContentProvider(), availability);
        return service.previewMode(service.createPreview(100L).getPreviewToken());
    }

    private FilePreviewRenderMode renderModeOf(FileApi fileApi, PreviewSourceAvailability availability) {
        FilePreviewServiceImpl service = service(properties(), fileApi, new StubTokenStore(), Clock.systemUTC(),
                new StubFileContentProvider(), availability);
        return service.createPreview(100L).getRenderMode();
    }

    @Test
    void createEnginePreview_关闭进程内直取时始终回退URL且不告警() {
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("http://business.example.com");
        properties.setInternalSourceEnabled(false);
        FilePreviewServiceImpl service = service(properties, new StubFileApi(), new StubTokenStore(),
                Clock.systemUTC(), new StubFileContentProvider(), () -> true);
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            String previewUrl = service.createEnginePreview(100L).getPreviewUrl();

            assertThat(previewUrl).contains("url=");
            assertThat(previewUrl).doesNotContain("fileId=");
            assertThat(appender.list).noneMatch(event -> event.getFormattedMessage().contains("未装配 FileDownloader"));
        } finally {
            detachLogAppender(appender);
        }
    }

    @Test
    void createEnginePreview_期望直取但进程无实现时告警回退() {
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("http://business.example.com");
        FilePreviewServiceImpl service = service(properties, new StubFileApi(), new StubTokenStore(),
                Clock.systemUTC(), new StubFileContentProvider());
        ListAppender<ILoggingEvent> appender = attachLogAppender();
        try {
            String previewUrl = service.createEnginePreview(100L).getPreviewUrl();

            assertThat(previewUrl).contains("url=");
            assertThat(previewUrl).doesNotContain("fileId=");
            assertThat(appender.list).anyMatch(event -> event.getFormattedMessage().contains("未装配 FileDownloader"));
        } finally {
            detachLogAppender(appender);
        }
    }

    private ListAppender<ILoggingEvent> attachLogAppender() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(FilePreviewServiceImpl.class)).addAppender(appender);
        return appender;
    }

    private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(FilePreviewServiceImpl.class)).detachAppender(appender);
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
        return service(properties(), new StubFileApi(), tokenStore, clock, contentProvider);
    }

    /**
     * url 兜底通道要求显式配置 {@code source-base-url}，测试默认给一个稳定的内部地址。
     */
    private FilePreviewProperties properties() {
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setSourceBaseUrl("http://127.0.0.1");
        return properties;
    }

    @Test
    void createPreview_返回渲染类型() {
        FilePreviewServiceImpl service = service(properties(), new StubFileApi("demo.docx"));

        assertThat(service.createPreview(100L).getRenderMode()).isEqualTo(FilePreviewRenderMode.CONVERTING);
        assertThat(service.createEnginePreview(100L).getRenderMode()).isEqualTo(FilePreviewRenderMode.CONVERTING);
    }

    @Test
    void renderMode_按格式映射渲染类型() {
        assertThat(renderMode("demo.pdf", null, null)).isEqualTo(FilePreviewRenderMode.PDF);
        assertThat(renderMode("demo.docx", null, null)).isEqualTo(FilePreviewRenderMode.CONVERTING);
        assertThat(renderMode("shot.JPEG", null, null)).isEqualTo(FilePreviewRenderMode.IMAGE);
        assertThat(renderMode("clip.mp4", null, null)).isEqualTo(FilePreviewRenderMode.VIDEO);
        assertThat(renderMode("song.mp3", null, null)).isEqualTo(FilePreviewRenderMode.AUDIO);
        assertThat(renderMode("song.wav", null, null)).isEqualTo(FilePreviewRenderMode.AUDIO);
        assertThat(renderMode("README", null, null)).isEqualTo(FilePreviewRenderMode.NOT_SUPPORTED);
        assertThat(renderMode("data.zip", null, null)).isEqualTo(FilePreviewRenderMode.PAGE);
        assertThat(renderMode("data.txt", null, null)).isEqualTo(FilePreviewRenderMode.PAGE);
    }

    @Test
    void renderMode_扩展名缺失时按PDF内容类型兜底() {
        assertThat(renderMode("README", "application/pdf", null)).isEqualTo(FilePreviewRenderMode.PDF);
        assertThat(renderMode("README", "application/octet-stream", null))
                .isEqualTo(FilePreviewRenderMode.NOT_SUPPORTED);
    }

    private FilePreviewRenderMode renderMode(String fileName, String contentType, String fileExt) {
        FilePreviewServiceImpl service = service(properties(), new StubFileApi(fileName, fileExt, contentType));
        return service.createPreview(100L).getRenderMode();
    }

    private FilePreviewServiceImpl service(FilePreviewProperties properties, FileApi fileApi) {
        return service(properties, fileApi, new StubTokenStore(), Clock.systemUTC(), new StubFileContentProvider());
    }

    private FilePreviewServiceImpl service(FilePreviewProperties properties,
                                           FileApi fileApi,
                                           StubTokenStore tokenStore,
                                           Clock clock,
                                           StubFileContentProvider contentProvider) {
        return service(properties, fileApi, tokenStore, clock, contentProvider, null);
    }

    private FilePreviewServiceImpl service(FilePreviewProperties properties,
                                           FileApi fileApi,
                                           StubTokenStore tokenStore,
                                           Clock clock,
                                           StubFileContentProvider contentProvider,
                                           PreviewSourceAvailability availability) {
        FilePreviewFileGateway gateway = new FilePreviewFileGateway(fileApi, contentProvider);
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        if (availability != null) {
            beanFactory.registerSingleton("previewSourceAvailability", availability);
        }
        return new FilePreviewServiceImpl(gateway, properties, tokenStore, new ObjectMapper(), clock,
                mock(IFilePreviewTaskService.class), beanFactory.getBeanProvider(PreviewSourceAvailability.class));
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
        private final String fileHash;
        private final String fileExt;
        private final String contentType;

        private StubFileApi() {
            this("demo.pptx", null);
        }

        private StubFileApi(String fileName) {
            this(fileName, null);
        }

        private StubFileApi(String fileName, String fileExt, String contentType) {
            this(fileName, null, fileExt, contentType);
        }

        private StubFileApi(String fileName, String fileHash) {
            this(fileName, fileHash, null, null);
        }

        private StubFileApi(String fileName, String fileHash, String fileExt, String contentType) {
            this.fileName = fileName;
            this.fileHash = fileHash;
            this.fileExt = fileExt;
            this.contentType = contentType;
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
            vo.setFileHash(fileHash);
            vo.setFileExt(fileExt);
            vo.setContentType(contentType);
            return R.ok(vo);
        }

        @Override
        public R<FileRecordVO> updateMetadata(io.mango.file.api.command.UpdateFileRecordCommand command) {
            throw new UnsupportedOperationException();
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

    private static final class LargeFileContentProvider extends StubFileContentProvider {
        @Override
        public FileDownloadVO downloadForService(Long id) {
            return new FileDownloadVO(new InputStream() {
                private long remaining = 200L * 1024 * 1024;
                @Override public int read() throws IOException {
                    if (remaining-- <= 0) return -1;
                    return 1;
                }
            }, "large.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 200L * 1024 * 1024);
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
