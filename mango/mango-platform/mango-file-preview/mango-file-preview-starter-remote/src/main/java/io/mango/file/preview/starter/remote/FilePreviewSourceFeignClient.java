package io.mango.file.preview.starter.remote;

import feign.Response;
import io.mango.common.contract.BinaryHttpAdapter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 预览源文件二进制传输客户端。
 *
 * <p>返回 {@link Response} 而不是 {@code ResponseEntity<byte[]>}：Feign 只在返回类型可缓冲时才读取整包，
 * 这里直接拿响应体流写盘，避免把整个文件读进内存。</p>
 */
@BinaryHttpAdapter
@FeignClient(name = "mango-file-preview", contextId = "filePreviewSourceFeignClient", path = "/file-preview",
        url = "${mango.file-preview.remote.base-url}",
        configuration = FilePreviewRemoteFeignConfiguration.class)
public interface FilePreviewSourceFeignClient {

    /**
     * 按票据读取源文件字节流。
     *
     * @param token 取源票据
     * @return 未解码的 Feign 响应，调用方必须关闭
     */
    @GetMapping("/sources")
    Response downloadSource(@RequestParam("token") String token);
}
