package cn.keking.utils;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.model.ReturnResponse;
import cn.keking.spi.PreviewSourceProvider;
import cn.keking.spi.PreviewSourceProviders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DownloadUtilsInternalSourceTest {

    private PreviewSourceProvider provider;

    @AfterEach
    void unregisterProvider() {
        PreviewSourceProviders.unregister(provider);
        provider = null;
    }

    @Test
    void downLoad_fileid地址直接读进程内文件不至HTTP() throws IOException {
        byte[] content = "internal-source-content".getBytes(StandardCharsets.UTF_8);
        previewContent(content);
        String fileName = uniqueFileName();

        ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute(fileName), fileName);

        assertThat(response.getCode()).isZero();
        assertThat(response.getContent()).isNotNull();
        assertThat(new File(response.getContent()))
                .exists()
                .hasContent("internal-source-content");
    }

    @Test
    void downLoad_没有直取实现时失败且不回退HTTP() {
        String fileName = uniqueFileName();

        ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute(fileName), fileName);

        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getMsg()).contains("直取实现");
    }

    @Test
    void downLoad_直取失败时返回失败而不是空文件() throws IOException {
        provider = sourceId -> {
            throw new IOException("令牌已过期");
        };
        PreviewSourceProviders.register(provider);
        String fileName = uniqueFileName();

        ReturnResponse<String> response = DownloadUtils.downLoad(fileAttribute(fileName), fileName);

        assertThat(response.getCode()).isEqualTo(1);
        assertThat(response.getMsg()).contains("令牌已过期");
    }

    private void previewContent(byte[] content) {
        provider = sourceId -> new ByteArrayInputStream(content);
        PreviewSourceProviders.register(provider);
    }

    private FileAttribute fileAttribute(String fileName) {
        return new FileAttribute(FileType.SIMTEXT, "txt", fileName, PreviewSourceUrl.build(fileName));
    }

    private String uniqueFileName() {
        return "internal-source-" + UUID.randomUUID() + ".txt";
    }
}
