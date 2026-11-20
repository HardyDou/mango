package io.mango.file.preview.starter.remote;

import io.mango.common.result.R;
import io.mango.file.preview.api.FilePreviewApi;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.api.vo.FilePreviewTaskVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 文件预览 JSON 接口的远程声明，供引擎进程按票据校验源文件可用性。
 */
@FeignClient(name = "mango-file-preview", contextId = "filePreviewFeignClient", path = "/file-preview",
        url = "${mango.file-preview.remote.base-url}",
        configuration = FilePreviewRemoteFeignConfiguration.class)
public interface FilePreviewFeignClient extends FilePreviewApi {

    @Override
    @GetMapping("/files/preview-link")
    R<FilePreviewLinkVO> preview(
            @RequestParam("fileId") Long fileId,
            @RequestParam(value = "allowLargeFile", defaultValue = "false") Boolean allowLargeFile);

    @Override
    @GetMapping("/files/preview-status")
    R<FilePreviewTaskVO> status(
            @RequestParam("fileId") Long fileId,
            @RequestParam(value = "allowLargeFile", defaultValue = "false") Boolean allowLargeFile);

    @Override
    @GetMapping("/sources/status")
    R<Boolean> sourceStatus(@RequestParam("token") String token);
}
