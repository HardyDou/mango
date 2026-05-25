package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.CompleteFileUploadPartCommand;
import io.mango.file.api.command.CreateFileUploadPartSignCommand;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileUploadInitVO;
import io.mango.file.api.vo.FileUploadPartSignVO;
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
@RestController("mangoFileController")
@RequestMapping("/file/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、下载、预览元数据、记录查询与归档接口")
public class FileController implements FileApi {

    private final IFileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "单文件上传", description = "权限接口。上传文件并创建当前机构下的文件记录")
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
            @RequestParam(required = false) String bizId,
            @Parameter(description = "业务自定义参数 JSON")
            @RequestParam(required = false) String bizMeta,
            @Parameter(description = "逻辑目录ID。根目录为0")
            @RequestParam(required = false) Long directoryId) {
        return fileService.upload(file, purpose, accessLevel, bizType, bizId, bizMeta, directoryId);
    }

    @PostMapping(path = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "多文件上传", description = "权限接口。批量上传文件并创建当前机构下的文件记录")
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
            @RequestParam(required = false) String bizId,
            @Parameter(description = "业务自定义参数 JSON")
            @RequestParam(required = false) String bizMeta,
            @Parameter(description = "逻辑目录ID。根目录为0")
            @RequestParam(required = false) Long directoryId) {
        return fileService.uploadBatch(files, purpose, accessLevel, bizType, bizId, bizMeta, directoryId);
    }

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:list")
    @Operation(summary = "分页查询文件记录", description = "权限接口。按当前登录机构查询文件记录")
    @Override
    public R<PageResult<FileRecordVO>> page(@ParameterObject FileRecordPageQuery query) {
        return fileService.page(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:query")
    @Operation(summary = "获取文件详情", description = "权限接口。按文件ID查询文件记录详情")
    @Override
    public R<FileRecordVO> get(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id) {
        return fileService.get(id);
    }

    @GetMapping("/preview")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:query")
    @Operation(summary = "获取文件预览元数据", description = "权限接口。返回文件名、类型、大小、预览地址和下载地址")
    @Override
    public R<FilePreviewVO> preview(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id) {
        return fileService.preview(id);
    }

    @Override
    public R<FileRecordVO> save(SaveFileCommand command) {
        return fileService.save(command);
    }

    @Override
    public FileDownloadVO download(Long id) {
        return fileService.download(id);
    }

    @Override
    public R<Boolean> archive(FileArchiveCommand command) {
        return fileService.archive(command);
    }

    @GetMapping("/download")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:download")
    @Operation(summary = "下载文件", description = "权限接口。按文件ID下载当前机构文件")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadResponse(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id) {
        FileDownloadVO download = fileService.download(id);
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
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:archive")
    @Operation(summary = "归档文件", description = "权限接口。默认只归档文件记录，不物理删除存储对象")
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
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:archive")
    @Operation(summary = "归档文件", description = "权限接口。按命令归档文件记录，不物理删除存储对象")
    public R<Boolean> archiveByCommand(
            @Parameter(description = "文件ID", required = true)
            @RequestParam Long id,
            @Valid @RequestBody(required = false) FileArchiveCommand command) {
        FileArchiveCommand resolved = command == null ? new FileArchiveCommand() : command;
        resolved.setId(id);
        return fileService.archive(resolved);
    }

    @PostMapping("/uploads")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "初始化分片上传", description = "权限接口。按当前生效存储配置创建上传会话，命中秒传时直接返回文件记录")
    public R<FileUploadInitVO> createUploadSession(
            @Valid @RequestBody CreateFileUploadSessionCommand command) {
        return fileService.createUploadSession(command);
    }

    @PostMapping("/uploads/{sessionId}/parts/sign")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "签发分片上传地址", description = "权限接口。为 MinIO/S3 原生分片上传生成浏览器可直传的预签名地址")
    public R<FileUploadPartSignVO> createUploadPartSign(
            @Parameter(description = "上传会话ID", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateFileUploadPartSignCommand command) {
        return fileService.createUploadPartSign(sessionId, command);
    }

    @PostMapping(path = "/uploads/{sessionId}/parts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "上传后端分片", description = "权限接口。用于不支持对象存储原生分片的存储类型，由后端接收分片并在完成时合并")
    public R<Boolean> uploadServerPart(
            @Parameter(description = "上传会话ID", required = true)
            @PathVariable Long sessionId,
            @Parameter(description = "分片序号，从 1 开始", required = true)
            @RequestParam Integer partNumber,
            @Parameter(description = "分片文件", required = true)
            @RequestPart("file") MultipartFile file) {
        return fileService.uploadServerPart(sessionId, partNumber, file);
    }

    @PutMapping("/uploads/{sessionId}/parts")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "登记分片完成", description = "权限接口。登记对象存储返回的分片 ETag 或后端分片元数据")
    public R<Boolean> completeUploadPart(
            @Parameter(description = "上传会话ID", required = true)
            @PathVariable Long sessionId,
            @Valid @RequestBody CompleteFileUploadPartCommand command) {
        return fileService.completeUploadPart(sessionId, command);
    }

    @PostMapping("/uploads/{sessionId}/complete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "完成分片上传", description = "权限接口。完成对象存储原生分片或后端分片合并，并创建文件记录")
    public R<FileRecordVO> completeUploadSession(
            @Parameter(description = "上传会话ID", required = true)
            @PathVariable Long sessionId) {
        return fileService.completeUploadSession(sessionId);
    }

    @DeleteMapping("/uploads/{sessionId}")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:upload")
    @Operation(summary = "取消分片上传", description = "权限接口。取消对象存储分片上传或清理后端临时分片")
    public R<Boolean> abortUploadSession(
            @Parameter(description = "上传会话ID", required = true)
            @PathVariable Long sessionId) {
        return fileService.abortUploadSession(sessionId);
    }
}
