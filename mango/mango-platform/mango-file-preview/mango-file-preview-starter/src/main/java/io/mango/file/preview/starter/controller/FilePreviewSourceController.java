package io.mango.file.preview.starter.controller;

import cn.keking.config.ConfigConstants;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.file.preview.core.service.IFilePreviewService;
import io.mango.file.preview.core.service.model.FilePreviewSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件预览源文件流适配器。
 */
@Validated
@RestController
@RequestMapping("/file-preview")
@RequiredArgsConstructor
@Tag(name = "文件预览源文件", description = "预览引擎短期源文件读取接口")
public class FilePreviewSourceController {

    private final IFilePreviewService filePreviewService;

    @GetMapping("/sources")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "读取预览源文件")
    @Operation(summary = "读取预览源文件", description = "临时令牌接口。仅供预览引擎在有效期内读取源文件")
    public ResponseEntity<InputStreamResource> source(
            @Parameter(description = "源文件访问令牌", required = true)
            @RequestParam("token") @NotBlank(message = "源文件访问令牌不能为空") String token) {
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

    @GetMapping("/generated")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "读取转换后的预览文件")
    @Operation(summary = "读取转换后的预览文件", description = "临时令牌接口。仅在源文件令牌有效期内读取转换结果")
    public ResponseEntity<FileSystemResource> generated(
            @Parameter(description = "源文件访问令牌", required = true)
            @RequestParam("token") @NotBlank(message = "源文件访问令牌不能为空") String token,
            @Parameter(description = "转换后 PDF 文件名", required = true)
            @RequestParam("fileName") @NotBlank(message = "转换后文件名不能为空") String fileName) {
        filePreviewService.validateGeneratedAccess(token, fileName);
        Path fileDir = Path.of(ConfigConstants.getFileDir()).toAbsolutePath().normalize();
        Path generatedFile = fileDir.resolve(fileName).normalize();
        if (!generatedFile.startsWith(fileDir) || !Files.isRegularFile(generatedFile)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        FileSystemResource resource = new FileSystemResource(generatedFile);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }
}
