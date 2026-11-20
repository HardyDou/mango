package io.mango.file.preview.starter.remote;

import feign.Response;
import io.mango.common.result.R;
import io.mango.file.preview.support.remote.FilePreviewSourceTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 基于 Feign 的远程取源传输实现：把引擎侧的取源请求转成回 Mango 的两次调用
 * （{@code GET /file-preview/sources/status} 校验票据、{@code GET /file-preview/sources} 取流）。
 */
public class FilePreviewRemoteSourceTransport implements FilePreviewSourceTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewRemoteSourceTransport.class);

    private final FilePreviewFeignClient previewFeignClient;

    private final FilePreviewSourceFeignClient sourceFeignClient;

    public FilePreviewRemoteSourceTransport(FilePreviewFeignClient previewFeignClient,
            FilePreviewSourceFeignClient sourceFeignClient) {
        this.previewFeignClient = previewFeignClient;
        this.sourceFeignClient = sourceFeignClient;
    }

    @Override
    public boolean isSourceTokenValid(String token) {
        R<Boolean> result = previewFeignClient.sourceStatus(token);
        return result != null && Boolean.TRUE.equals(result.getData());
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
