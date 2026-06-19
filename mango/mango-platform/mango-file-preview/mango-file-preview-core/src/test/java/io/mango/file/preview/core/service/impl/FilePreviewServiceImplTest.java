package io.mango.file.preview.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.file.api.FileApi;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.preview.core.config.FilePreviewProperties;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.kv.api.ITokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

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
    void openSource_使用服务内下载读取源文件() {
        FilePreviewServiceImpl service = service();
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        var preview = service.createPreview(100L);
        var enginePreview = service.createEnginePreviewByToken(preview.getPreviewToken());
        String token = enginePreview.getPreviewUrl().substring(enginePreview.getPreviewUrl().indexOf("url=") + 4);
        token = java.net.URLDecoder.decode(token, java.nio.charset.StandardCharsets.UTF_8);
        String sourceUrl = new String(java.util.Base64.getDecoder().decode(token), java.nio.charset.StandardCharsets.UTF_8);
        String sourceToken = sourceUrl.substring(sourceUrl.lastIndexOf('/') + 1, sourceUrl.indexOf('?'));

        var source = service.openSource(sourceToken);

        assertThat(source.fileName()).isEqualTo("demo.pptx");
        assertThat(source.contentLength()).isEqualTo(1L);
    }

    private FilePreviewServiceImpl service() {
        FilePreviewProperties properties = new FilePreviewProperties();
        return new FilePreviewServiceImpl(new StubFileApi(), properties, new StubTokenStore(),
                new ObjectMapper(), Clock.systemUTC());
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

        @Override
        public R<io.mango.common.vo.PageResult<FileRecordVO>> page(io.mango.file.api.query.FileRecordPageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public R<FileRecordVO> get(Long id) {
            FileRecordVO vo = new FileRecordVO();
            vo.setId(id);
            vo.setFileName("demo.pptx");
            return R.ok(vo);
        }

        @Override
        public R<io.mango.file.api.vo.FilePreviewVO> preview(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileDownloadVO download(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileDownloadVO downloadForService(Long id) {
            return new FileDownloadVO(new ByteArrayInputStream(new byte[]{1}), "demo.pptx",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation", 1L);
        }

        @Override
        public R<Boolean> archive(io.mango.file.api.command.FileArchiveCommand command) {
            throw new UnsupportedOperationException();
        }
    }
}
