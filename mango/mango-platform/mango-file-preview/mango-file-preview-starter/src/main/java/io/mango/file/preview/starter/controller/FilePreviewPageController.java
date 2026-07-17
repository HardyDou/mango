package io.mango.file.preview.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * 文件预览页面适配器。
 */
@Validated
@RestController
@RequestMapping("/file-preview")
@RequiredArgsConstructor
@Tag(name = "文件预览页面", description = "文件预览页面跳转接口")
public class FilePreviewPageController {

    private final IFilePreviewService filePreviewService;

    @GetMapping("/files/preview")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:download")
    @Operation(summary = "跳转文件预览页", description = "权限接口。按文件ID跳转到当前租户可见文件的在线预览页面")
    public ModelAndView redirectPreview(
            @Parameter(description = "文件ID", required = true)
            @RequestParam("fileId") @NotNull(message = "文件ID不能为空") Long fileId) {
        FilePreviewLinkVO link = filePreviewService.createEnginePreview(fileId);
        return new ModelAndView("forward:" + link.getPreviewUrl());
    }

    @GetMapping("/files/preview-entry")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "文件预览临时入口")
    @Operation(summary = "跳转临时文件预览页", description = "公开接口。使用已鉴权接口签发的短期令牌跳转到在线预览页面")
    public ModelAndView redirectPreviewEntry(
            @Parameter(description = "预览入口临时令牌", required = true)
            @RequestParam("token") @NotBlank(message = "预览入口临时令牌不能为空") String token) {
        FilePreviewLinkVO link = filePreviewService.createEnginePreviewByToken(token);
        return new ModelAndView("forward:" + link.getPreviewUrl());
    }
}
