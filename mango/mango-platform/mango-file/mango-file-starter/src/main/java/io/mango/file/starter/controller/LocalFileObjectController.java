package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorageRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * 本地磁盘对象访问接口。
 */
@RestController
@RequestMapping("/file/local-objects")
@RequiredArgsConstructor
@Tag(name = "本地文件对象", description = "本地磁盘存储对象公开读取接口")
public class LocalFileObjectController {

    private final FileStorageRouter fileStorageRouter;

    @GetMapping("/{bucket}/**")
    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "本地磁盘存储对象读取")
    @Operation(summary = "读取本地磁盘对象", description = "公开接口。为本地磁盘存储 DIRECT 访问模式提供对象读取能力")
    public ResponseEntity<InputStreamResource> get(
            @Parameter(description = "存储桶", required = true)
            @PathVariable String bucket,
            @RequestParam(required = false, defaultValue = "false") boolean download,
            jakarta.servlet.http.HttpServletRequest request) {
        String objectName = objectName(request, bucket);
        FileStorageConfig config = localConfig(bucket);
        FileObject object = fileStorageRouter.getObject(config, objectName);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (object.contentType() != null && !object.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(object.contentType());
        }
        ContentDisposition disposition = download
                ? ContentDisposition.attachment().filename(fileName(objectName), StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(fileName(objectName), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentLength(object.contentLength())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(object.inputStream()));
    }

    private FileStorageConfig localConfig(String bucket) {
        FileStorageConfig config = new FileStorageConfig();
        config.setStorageType("LOCAL");
        config.setBucketName(bucket);
        return config;
    }

    private String objectName(jakarta.servlet.http.HttpServletRequest request, String bucket) {
        String path = request.getRequestURI();
        String prefix = request.getContextPath() + "/file/local-objects/" + bucket + "/";
        String objectName = path.startsWith(prefix) ? path.substring(prefix.length()) : "";
        return UriUtils.decode(objectName, StandardCharsets.UTF_8);
    }

    private String fileName(String objectName) {
        int index = objectName.lastIndexOf('/');
        return index >= 0 ? objectName.substring(index + 1) : objectName;
    }
}
