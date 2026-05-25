package io.mango.file.preview.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.file.preview.api.FilePreviewApi;
import io.mango.file.preview.api.vo.FilePreviewLinkVO;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

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
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:query")
    @Operation(summary = "创建文件预览链接", description = "权限接口。按文件ID创建在线预览页面地址")
    public R<FilePreviewLinkVO> preview(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long fileId) {
        return R.ok(filePreviewService.createPreview(fileId));
    }

    @GetMapping("/files/preview")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:query")
    @Operation(summary = "跳转文件预览页", description = "权限接口。按文件ID跳转到在线预览页面")
    public ModelAndView redirectPreview(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long fileId) {
        FilePreviewLinkVO link = filePreviewService.createEnginePreview(fileId);
        return new ModelAndView("forward:" + link.getPreviewUrl());
    }

    @GetMapping("/files/preview-entry")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "文件预览临时入口")
    @Operation(summary = "跳转临时文件预览页", description = "公开接口。使用已鉴权接口签发的短期令牌跳转到在线预览页面")
    public ModelAndView redirectPreviewEntry(
            @Parameter(description = "预览入口临时令牌", required = true)
            @RequestParam String token) {
        FilePreviewLinkVO link = filePreviewService.createEnginePreviewByToken(token);
        return new ModelAndView("forward:" + link.getPreviewUrl());
    }

    @GetMapping("/sources/{token}")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "读取预览源文件")
    @Operation(summary = "读取预览源文件", description = "临时令牌接口。仅供预览引擎在有效期内读取源文件")
    public ResponseEntity<InputStreamResource> source(
            @Parameter(description = "源文件访问令牌", required = true)
            @PathVariable String token) {
        FilePreviewSource source = filePreviewService.openSource(token);
        String filename = UriUtils.encode(source.fileName(), StandardCharsets.UTF_8);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (source.contentType() != null && !source.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(source.contentType());
        }
        return ResponseEntity.ok()
                .contentLength(source.contentLength())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(source.inputStream()));
    }
}
