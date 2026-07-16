package io.mango.file.preview.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.file.preview.api.FilePreviewApi;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件预览接口。
 */
@Validated
@RestController
@RequestMapping("/file-preview")
@RequiredArgsConstructor
@Tag(name = "文件预览", description = "文件 ID 在线预览与临时源文件访问接口")
public class FilePreviewController implements FilePreviewApi {

    private final IFilePreviewService filePreviewService;

    @Override
    @GetMapping("/files/preview-link")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "创建文件预览链接", description = "登录接口。按文件ID创建当前租户可见文件的在线预览页面地址")
    public R<FilePreviewLinkVO> preview(
            @Parameter(description = "文件ID", required = true)
            @RequestParam("fileId") Long fileId) {
        return R.ok(filePreviewService.createPreview(fileId));
    }

}
