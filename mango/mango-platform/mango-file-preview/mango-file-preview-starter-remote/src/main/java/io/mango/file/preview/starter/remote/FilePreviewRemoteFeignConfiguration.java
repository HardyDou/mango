package io.mango.file.preview.starter.remote;

import feign.Request;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 预览取源 Feign 客户端专属配置。
 *
 * <p>刻意不加 {@code @Configuration}：本类只通过 {@code @FeignClient(configuration = ...)} 注入子上下文，
 * 避免把取源专用超时注册成全局 {@link Request.Options} 影响其它模块的 Feign 调用。</p>
 */
public class FilePreviewRemoteFeignConfiguration {

    /**
     * 取源专用超时：连接/读取超时都来自 {@code mango.file-preview.remote.*}。
     *
     * @param properties 远程取源配置
     * @return Feign 超时选项
     */
    @Bean
    @ConditionalOnMissingBean
    public Request.Options filePreviewRemoteRequestOptions(FilePreviewRemoteProperties properties) {
        return new Request.Options(properties.getConnectTimeout(), properties.getReadTimeout(), true);
    }
}
