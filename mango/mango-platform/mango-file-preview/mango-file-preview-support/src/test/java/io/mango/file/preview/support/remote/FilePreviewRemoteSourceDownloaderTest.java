package io.mango.file.preview.support.remote;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.spi.FileDownloaders;
import cn.keking.utils.DownloadUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilePreviewRemoteSourceDownloaderTest {

    @TempDir
    Path tempDir;

    private String originalFileDir;

    private FilePreviewSourceTransport transport;

    private FilePreviewRemoteSourceDownloader downloader;

    @BeforeEach
    void setUp() {
        originalFileDir = ConfigConstants.getFileDir();
        ConfigConstants.setFileDirValue(tempDir.toString());
        transport = mock(FilePreviewSourceTransport.class);
        downloader = new FilePreviewRemoteSourceDownloader(transport);
    }

    @AfterEach
    void tearDown() {
        if (downloader != null) {
            downloader.destroy();
        }
        ConfigConstants.setFileDirValue(originalFileDir);
    }

    @Test
    void verify_委托远程校验票据() {
        when(transport.isSourceTokenValid("ticket-1")).thenReturn(true);
        when(transport.isSourceTokenValid("ticket-2")).thenReturn(false);

        assertThat(downloader.verify("ticket-1")).isTrue();
        assertThat(downloader.verify("ticket-2")).isFalse();
    }

    @Test
    void verify_远程校验异常时按未确认处理() {
        when(transport.isSourceTokenValid("ticket-1")).thenThrow(new IllegalStateException("mango 不可达"));

        assertThat(downloader.verify("ticket-1")).isFalse();
    }

    @Test
    void verify_空票据不再发起远程调用() {
        assertThat(downloader.verify(null)).isFalse();
        assertThat(downloader.verify("  ")).isFalse();
    }

    @Test
    void supports_只接管带源标识的属性() {
        FileAttribute withoutSourceId = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", "http://x/demo.txt");

        assertThat(downloader.supports(attribute("source-1"))).isTrue();
        assertThat(downloader.supports(withoutSourceId)).isFalse();
        assertThat(downloader.supports(null)).isFalse();
    }

    @Test
    void download_把远程源文件流式写到引擎目录() throws IOException {
        stubDownloadWrites("remote-content");

        ReturnResponse<String> response = downloader.download(attribute("source-1"), "demo.txt");

        assertThat(response.getCode()).isZero();
        assertThat(new File(response.getContent()))
                .exists()
                .hasContent("remote-content");
    }

    @Test
    void download_远程失败时返回失败而不是空文件() throws IOException {
        doThrow(new IOException("读取超时")).when(transport).downloadSource(eq("source-1"), any(File.class));

        ReturnResponse<String> response = downloader.download(attribute("source-1"), "demo.txt");

        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getMsg()).isEqualTo("下载失败: 读取超时");
    }

    @Test
    void afterPropertiesSet_注册后引擎默认入口走本下载器() throws IOException {
        stubDownloadWrites("registered");
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

    private void stubDownloadWrites(String content) throws IOException {
        doAnswer(invocation -> {
            File target = invocation.getArgument(1);
            Files.write(target.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(transport).downloadSource(eq("source-1"), any(File.class));
    }

    private FileAttribute attribute(String sourceId) {
        FileAttribute attribute = new FileAttribute(FileType.SIMTEXT, "txt", "demo.txt", null);
        attribute.setSourceId(sourceId);
        return attribute;
    }
}
