package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.SaveFileStorageConfigCommand;
import io.mango.file.api.command.TestFileStorageConfigCommand;
import io.mango.file.api.query.FileStorageConfigPageQuery;
import io.mango.file.api.vo.FileStorageConfigTestVO;
import io.mango.file.api.vo.FileStorageConfigVO;
import io.mango.file.core.service.IFileStorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件存储配置管理接口。
 */
@RestController
@RequestMapping("/file/storage-configs")
@RequiredArgsConstructor
@Tag(name = "文件存储配置", description = "文件第三方存储配置列表、详情、新增、修改、删除、启用与连接测试接口")
public class FileStorageConfigController {

    private final IFileStorageConfigService storageConfigService;

    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:file-storage:list")
    @Operation(summary = "分页查询文件存储配置", description = "权限接口。分页查询平台级文件存储配置，配置本身不按机构隔离")
    public R<PageResult<FileStorageConfigVO>> page(@ParameterObject FileStorageConfigPageQuery query) {
        return storageConfigService.page(query);
    }

    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:file-storage:query")
    @Operation(summary = "获取文件存储配置详情", description = "权限接口。按配置ID查询文件存储配置详情，SecretKey 不会明文返回")
    public R<FileStorageConfigVO> get(
            @Parameter(description = "存储配置ID", required = true)
            @RequestParam Long id) {
        return storageConfigService.get(id);
    }

    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:file-storage:add")
    @Operation(summary = "新增文件存储配置", description = "权限接口。创建本地、S3兼容、MinIO、AWS S3、阿里云OSS、腾讯云COS或七牛云Kodo存储配置")
    public R<Long> create(
            @Parameter(description = "保存文件存储配置命令", required = true)
            @Valid @RequestBody SaveFileStorageConfigCommand command) {
        return storageConfigService.create(command);
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:file-storage:edit")
    @Operation(summary = "修改文件存储配置", description = "权限接口。修改文件存储配置，SecretKey 为空表示保持原值")
    public R<Boolean> update(
            @Parameter(description = "保存文件存储配置命令", required = true)
            @Valid @RequestBody SaveFileStorageConfigCommand command) {
        return storageConfigService.update(command);
    }

    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:file-storage:delete")
    @Operation(summary = "删除文件存储配置", description = "权限接口。删除非默认启用的文件存储配置")
    public R<Boolean> delete(
            @Parameter(description = "存储配置ID", required = true)
            @RequestParam Long id) {
        return storageConfigService.delete(id);
    }

    @PutMapping("/active")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:file-storage:active")
    @Operation(summary = "启用默认文件存储配置", description = "权限接口。将指定配置设为默认启用配置，后续上传会使用该配置")
    public R<Boolean> activate(
            @Parameter(description = "存储配置ID", required = true)
            @RequestParam Long id) {
        return storageConfigService.activate(id);
    }

    @PostMapping("/test")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "system:file-storage:test")
    @Operation(summary = "测试文件存储配置", description = "权限接口。测试已保存配置或临时配置的连接可用性")
    public R<FileStorageConfigTestVO> test(
            @Parameter(description = "测试文件存储配置命令", required = true)
            @Valid @RequestBody TestFileStorageConfigCommand command) {
        return storageConfigService.test(command);
    }
}
