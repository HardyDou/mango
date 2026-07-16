package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileStorageConfigApi;
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
import org.springframework.validation.annotation.Validated;

/**
 * 文件存储配置管理接口。
 */
@RestController
@RequestMapping("/file/storage-configs")
@RequiredArgsConstructor
@Tag(name = "文件存储配置", description = "文件第三方存储配置列表、详情、新增、修改、删除、启用与连接测试接口")
@Validated
public class FileStorageConfigController implements FileStorageConfigApi {

    private final IFileStorageConfigService storageConfigService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:storage-configs:list")
    @Operation(summary = "分页查询文件存储配置", description = "权限接口。分页查询平台级文件存储配置，配置本身不按机构隔离")
    public R<PageResult<FileStorageConfigVO>> page(@ParameterObject FileStorageConfigPageQuery query) {
        return R.ok(storageConfigService.page(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:storage-configs:query")
    @Operation(summary = "获取文件存储配置详情", description = "权限接口。按配置ID查询文件存储配置详情，SecretKey 不会明文返回")
    public R<FileStorageConfigVO> get(
            @Parameter(description = "存储配置ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(storageConfigService.get(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:storage-configs:add")
    @Operation(summary = "新增文件存储配置", description = "权限接口。创建本地、S3兼容、MinIO、AWS S3、阿里云OSS、腾讯云COS或七牛云Kodo存储配置")
    public R<Long> create(
            @RequestBody SaveFileStorageConfigCommand command) {
        return R.ok(storageConfigService.create(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:storage-configs:edit")
    @Operation(summary = "修改文件存储配置", description = "权限接口。修改文件存储配置，SecretKey 为空表示保持原值")
    public R<Boolean> update(
            @RequestBody SaveFileStorageConfigCommand command) {
        return R.ok(storageConfigService.update(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:storage-configs:delete")
    @Operation(summary = "删除文件存储配置", description = "权限接口。删除非默认启用的文件存储配置")
    public R<Boolean> delete(
            @Parameter(description = "存储配置ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(storageConfigService.delete(id));
    }

    @Override
    @PutMapping("/active")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:storage-configs:active")
    @Operation(summary = "启用默认文件存储配置", description = "权限接口。将指定配置设为默认启用配置，后续上传会使用该配置")
    public R<Boolean> activate(
            @Parameter(description = "存储配置ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(storageConfigService.activate(id));
    }

    @Override
    @PostMapping("/test")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:storage-configs:test")
    @Operation(summary = "测试文件存储配置", description = "权限接口。测试已保存配置或临时配置的连接可用性")
    public R<FileStorageConfigTestVO> test(
            @RequestBody TestFileStorageConfigCommand command) {
        return R.ok(storageConfigService.test(command));
    }
}
