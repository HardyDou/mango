package io.mango.file.preview.starter;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.spi.FileDownloaders;
import cn.keking.utils.DownloadUtils;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilePreviewEngineDownloaderTest {

    @TempDir
    Path tempDir;

    private String originalFileDir;

    private IFilePreviewService filePreviewService;

    private FilePreviewEngineDownloader downloader;

    @BeforeEach
    void setUp() {
        originalFileDir = ConfigConstants.getFileDir();
        ConfigConstants.setFileDirValue(tempDir.toString());
        filePreviewService = mock(IFilePreviewService.class);
        downloader = new FilePreviewEngineDownloader(filePreviewService);
    }

    @AfterEach
    void tearDown() {
        if (downloader != null) {
            downloader.destroy();
        }
        ConfigConstants.setFileDirValue(originalFileDir);
    }

    @Test
    void verify_委托服务判断票据是否仍可用() {
        when(filePreviewService.isSourceTokenValid("ticket-1")).thenReturn(true);
        when(filePreviewService.isSourceTokenValid("ticket-2")).thenReturn(false);

        assertThat(downloader.verify("ticket-1")).isTrue();
        assertThat(downloader.verify("ticket-2")).isFalse();
    }

    @Test
    void supports_只接管带源标识的属性() {
        FileAttribute withSourceId = attribute("source-1");
        FileAttribute withoutSourceId = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", "http://x/demo.txt");

        assertThat(downloader.supports(withSourceId)).isTrue();
        assertThat(downloader.supports(withoutSourceId)).isFalse();
        assertThat(downloader.supports(null)).isFalse();
    }

    @Test
    void download_把进程内源文件写到引擎目录() throws IOException {
        when(filePreviewService.openSource("source-1")).thenReturn(new FilePreviewSource(
                new ByteArrayInputStream("preview-content".getBytes(StandardCharsets.UTF_8)),
                "demo.txt", "text/plain", 15L));

        ReturnResponse<String> response = downloader.download(attribute("source-1"), "demo.txt");

        assertThat(response.getCode()).isZero();
        assertThat(new File(response.getContent()))
                .exists()
                .hasContent("preview-content");
    }

    @Test
    void download_读取失败时返回失败而不是空文件() {
        when(filePreviewService.openSource("source-1")).thenThrow(new IllegalStateException("预览令牌无效或已过期"));

        ReturnResponse<String> response = downloader.download(attribute("source-1"), "demo.txt");

        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getMsg()).contains("预览令牌无效或已过期");
    }

    @Test
    void afterPropertiesSet_注册后引擎默认入口走本下载器() throws IOException {
        when(filePreviewService.openSource("source-1")).thenReturn(new FilePreviewSource(
                new ByteArrayInputStream("registered".getBytes(StandardCharsets.UTF_8)),
                "demo.txt", "text/plain", 10L));
        downloader.afterPropertiesSet();

        ReturnResponse<String> response = DownloadUtils.downLoad(attribute("source-1"), "demo.txt");

        assertThat(response.getCode()).isZero();
        assertThat(new File(response.getContent())).hasContent("registered");
    }

    @Test
    void destroy_注销后不再接管() {
        downloader.afterPropertiesSet();
        downloader.destroy();

        assertThat(FileDownloaders.tryDownload(attribute("source-1"), "demo.txt")).isNull();
    }

    @Test
    void internalSourceAvailable_内嵌引擎恒为真() {
        assertThat(downloader.internalSourceAvailable()).isTrue();
    }

    private FileAttribute attribute(String sourceId) {
        FileAttribute attribute = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", null);
        attribute.setSourceId(sourceId);
        return attribute;
    }
}
