package io.mango.file.preview.starter.remote;

import feign.Response;
import io.mango.file.preview.support.remote.FilePreviewSourceTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 基于 Feign 的远程取源传输实现：把引擎侧的取源请求转成一次
 * {@code GET /file-preview/sources?token=...} 调用，直接流式写盘。
 *
 * <p>不做票据有效性探测：票据本身就是凭据，无效或过期时由 Mango 侧取流接口拒绝，
 * 在引擎侧表现为下载失败，而不是对外多开一个探测口。</p>
 */
public class FilePreviewRemoteSourceTransport implements FilePreviewSourceTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewRemoteSourceTransport.class);

    private final FilePreviewSourceFeignClient sourceFeignClient;

    public FilePreviewRemoteSourceTransport(FilePreviewSourceFeignClient sourceFeignClient) {
        this.sourceFeignClient = sourceFeignClient;
    }

    @Override
    public void downloadSource(String token, File target) throws IOException {
        try (Response response = sourceFeignClient.downloadSource(token)) {
            if (response.body() == null) {
                throw new IOException("预览源文件响应为空");
            }
            LOGGER.debug("远程取源开始，目标文件：{}", target.getName());
            try (InputStream inputStream = response.body().asInputStream()) {
                Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
