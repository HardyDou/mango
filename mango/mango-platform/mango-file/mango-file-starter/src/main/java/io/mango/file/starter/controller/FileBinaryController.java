package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.file.api.command.CompleteFileUploadPartCommand;
import io.mango.file.api.command.CreateFileUploadPartSignCommand;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileUploadInitVO;
import io.mango.file.api.vo.FileUploadPartSignVO;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.model.FileDownloadOptions;
import io.mango.file.core.service.model.ServerFilePart;
import io.mango.common.contract.BinaryHttpAdapter;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Binary upload, download and multipart-upload protocol endpoints. */
@BinaryHttpAdapter
@RestController
@RequestMapping("/file/files")
@RequiredArgsConstructor
@Validated
@Tag(name = "文件传输", description = "文件上传、下载、内容预览与分片上传接口")
public class FileBinaryController {

    private final IFileService fileService;

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "单文件上传", description = "上传文件并创建当前机构下的文件记录")
    public R<FileRecordVO> upload(
            @Parameter(description = "文件", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "文件用途") @RequestParam(value = "purpose", required = false) String purpose,
            @Parameter(description = "访问级别") @RequestParam(value = "accessLevel", required = false) String accessLevel,
            @Parameter(description = "业务类型") @RequestParam(value = "bizType", required = false) String bizType,
            @Parameter(description = "业务ID") @RequestParam(value = "bizId", required = false) String bizId,
            @Parameter(description = "业务自定义参数") @RequestParam(value = "bizMeta", required = false) String bizMeta,
            @Parameter(description = "逻辑目录ID") @RequestParam(value = "directoryId", required = false) Long directoryId) {
        return R.ok(fileService.upload(file, uploadCommand(purpose, accessLevel, bizType, bizId, bizMeta, directoryId)));
    }

    @PostMapping("/batch")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "多文件上传", description = "批量上传文件并创建当前机构下的文件记录")
    public R<List<FileRecordVO>> uploadBatch(
            @Parameter(description = "文件列表", required = true) @RequestPart("files") MultipartFile[] files,
            @Parameter(description = "文件用途") @RequestParam(value = "purpose", required = false) String purpose,
            @Parameter(description = "访问级别") @RequestParam(value = "accessLevel", required = false) String accessLevel,
            @Parameter(description = "业务类型") @RequestParam(value = "bizType", required = false) String bizType,
            @Parameter(description = "业务ID") @RequestParam(value = "bizId", required = false) String bizId,
            @Parameter(description = "业务自定义参数") @RequestParam(value = "bizMeta", required = false) String bizMeta,
            @Parameter(description = "逻辑目录ID") @RequestParam(value = "directoryId", required = false) Long directoryId) {
        return R.ok(fileService.uploadBatch(files, uploadCommand(purpose, accessLevel, bizType, bizId, bizMeta, directoryId)));
    }

    @GetMapping("/download")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "下载文件", description = "按文件ID流式下载当前租户可见文件")
    public ResponseEntity<InputStreamResource> download(
            @Parameter(description = "文件ID", required = true) @RequestParam("id") Long id,
            @Parameter(description = "压缩档位") @RequestParam(value = "compression", required = false) String compression,
            @Parameter(description = "单文件目标大小") @RequestParam(value = "perFileTargetSizeBytes", required = false) Long targetSize) {
        return response(fileService.download(new FileDownloadOptions(id, compression, targetSize)), true);
    }

    @GetMapping("/preview-content")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "预览文件原始内容", description = "按文件ID以内联方式流式读取当前租户可见文件")
    public ResponseEntity<InputStreamResource> previewContent(
            @Parameter(description = "文件ID", required = true) @RequestParam("id") Long id) {
        return response(fileService.download(id), false);
    }

    @PostMapping("/uploads")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "初始化分片上传", description = "按当前生效存储配置创建上传会话")
    public R<FileUploadInitVO> createUploadSession(@RequestBody CreateFileUploadSessionCommand command) {
        return R.ok(fileService.createUploadSession(command));
    }

    @PostMapping("/uploads/{sessionId}/parts/sign")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "签发分片上传地址", description = "为对象存储原生分片上传生成预签名地址")
    public R<FileUploadPartSignVO> createUploadPartSign(
            @Parameter(description = "上传会话ID", required = true) @PathVariable Long sessionId,
            @RequestBody CreateFileUploadPartSignCommand command) {
        return R.ok(fileService.createUploadPartSign(sessionId, command));
    }

    @PostMapping("/uploads/{sessionId}/parts")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "上传后端分片", description = "由后端接收并保存一个上传分片")
    public R<Boolean> uploadServerPart(
            @Parameter(description = "上传会话ID", required = true) @PathVariable Long sessionId,
            @Parameter(description = "分片序号", required = true) @RequestParam("partNumber") Integer partNumber,
            @Parameter(description = "分片文件", required = true) @RequestPart("file") MultipartFile file) {
        return R.ok(fileService.uploadServerPart(new ServerFilePart(sessionId, partNumber, file)));
    }

    @PutMapping("/uploads/{sessionId}/parts")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "登记分片完成", description = "登记对象存储返回的分片元数据")
    public R<Boolean> completeUploadPart(
            @Parameter(description = "上传会话ID", required = true) @PathVariable Long sessionId,
            @RequestBody CompleteFileUploadPartCommand command) {
        return R.ok(fileService.completeUploadPart(sessionId, command));
    }

    @PostMapping("/uploads/{sessionId}/complete")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "完成分片上传", description = "完成分片合并并创建文件记录")
    public R<FileRecordVO> completeUploadSession(
            @Parameter(description = "上传会话ID", required = true) @PathVariable Long sessionId) {
        return R.ok(fileService.completeUploadSession(sessionId));
    }

    @DeleteMapping("/uploads/{sessionId}")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "取消分片上传", description = "取消对象存储分片上传或清理临时分片")
    public R<Boolean> abortUploadSession(
            @Parameter(description = "上传会话ID", required = true) @PathVariable Long sessionId) {
        return R.ok(fileService.abortUploadSession(sessionId));
    }

    private ResponseEntity<InputStreamResource> response(FileDownloadVO download, boolean attachment) {
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = download.contentType() == null || download.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.contentType());
        return ResponseEntity.ok()
                .contentLength(download.contentLength())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(download.inputStream()));
    }

    private SaveFileCommand uploadCommand(String purpose,
                                          String accessLevel,
                                          String bizType,
                                          String bizId,
                                          String bizMeta,
                                          Long directoryId) {
        SaveFileCommand command = new SaveFileCommand();
        command.setPurpose(purpose);
        command.setAccessLevel(accessLevel);
        command.setBizType(bizType);
        command.setBizId(bizId);
        command.setBizMeta(bizMeta);
        command.setDirectoryId(directoryId);
        return command;
    }
}
