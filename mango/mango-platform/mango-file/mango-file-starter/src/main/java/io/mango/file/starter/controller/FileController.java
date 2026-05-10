package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.FileDownload;
import io.mango.file.core.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件管理接口。
 */
@RestController
@RequestMapping("/file/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、下载、预览元数据、记录查询与归档接口")
public class FileController {

    private final IFileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "单文件上传")
    @Operation(summary = "单文件上传", description = "登录接口。上传文件并创建当前机构下的文件记录")
    public R<FileRecordVO> upload(
            @Parameter(description = "文件", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "文件用途，例如 avatar、attachment、contract")
            @RequestParam(required = false) String purpose,
            @Parameter(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL。默认 PRIVATE")
            @RequestParam(required = false) String accessLevel,
            @Parameter(description = "业务类型")
            @RequestParam(required = false) String bizType,
            @Parameter(description = "业务ID")
            @RequestParam(required = false) String bizId) {
        return fileService.upload(file, purpose, accessLevel, bizType, bizId);
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "多文件上传")
    @Operation(summary = "多文件上传", description = "登录接口。批量上传文件并创建当前机构下的文件记录")
    public R<List<FileRecordVO>> uploadBatch(
            @Parameter(description = "文件列表", required = true)
            @RequestPart("files") MultipartFile[] files,
            @Parameter(description = "文件用途，例如 avatar、attachment、contract")
            @RequestParam(required = false) String purpose,
            @Parameter(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL。默认 PRIVATE")
            @RequestParam(required = false) String accessLevel,
            @Parameter(description = "业务类型")
            @RequestParam(required = false) String bizType,
            @Parameter(description = "业务ID")
            @RequestParam(required = false) String bizId) {
        return fileService.uploadBatch(files, purpose, accessLevel, bizType, bizId);
    }

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "分页查询文件记录")
    @Operation(summary = "分页查询文件记录", description = "登录接口。按当前登录机构查询文件记录")
    public R<PageResult<FileRecordVO>> page(@ParameterObject FileRecordPageQuery query) {
        return fileService.page(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取文件详情")
    @Operation(summary = "获取文件详情", description = "登录接口。按文件ID查询文件记录详情")
    public R<FileRecordVO> get(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id) {
        return fileService.get(id);
    }

    @GetMapping("/preview")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取文件预览元数据")
    @Operation(summary = "获取文件预览元数据", description = "登录接口。返回文件名、类型、大小、预览地址和下载地址")
    public R<FilePreviewVO> preview(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id) {
        return fileService.preview(id);
    }

    @GetMapping("/download")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "下载文件")
    @Operation(summary = "下载文件", description = "登录接口。按文件ID下载当前机构文件")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> download(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id) {
        FileDownload download = fileService.download(id);
        String filename = UriUtils.encode(download.fileName(), StandardCharsets.UTF_8);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.contentType() != null && !download.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(download.contentType());
        }
        return ResponseEntity.ok()
                .contentLength(download.contentLength())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new org.springframework.core.io.InputStreamResource(download.inputStream()));
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "归档文件")
    @Operation(summary = "归档文件", description = "登录接口。默认只归档文件记录，不物理删除存储对象")
    public R<Boolean> archive(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id,
            @Parameter(description = "归档原因")
            @RequestParam(required = false) String reason) {
        FileArchiveCommand command = new FileArchiveCommand();
        command.setId(id);
        command.setReason(reason);
        return fileService.archive(command);
    }

    @PutMapping("/archive")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "归档文件")
    @Operation(summary = "归档文件", description = "登录接口。按命令归档文件记录，不物理删除存储对象")
    public R<Boolean> archiveByCommand(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id,
            @Valid @RequestBody(required = false) FileArchiveCommand command) {
        FileArchiveCommand resolved = command == null ? new FileArchiveCommand() : command;
        resolved.setId(id);
        return fileService.archive(resolved);
    }
}
