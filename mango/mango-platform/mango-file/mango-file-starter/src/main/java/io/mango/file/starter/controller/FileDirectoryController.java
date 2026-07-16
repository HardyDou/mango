package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.file.api.FileDirectoryApi;
import io.mango.file.api.command.SaveFileDirectoryCommand;
import io.mango.file.api.vo.FileDirectoryVO;
import io.mango.file.core.service.IFileDirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 文件逻辑目录接口。
 */
@RestController
@RequestMapping("/file/directories")
@RequiredArgsConstructor
@Tag(name = "文件逻辑目录", description = "文件中心逻辑目录树、新增、修改、删除接口")
@Validated
public class FileDirectoryController implements FileDirectoryApi {

    private final IFileDirectoryService directoryService;

    @Override
    @GetMapping("/tree")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:directories:list")
    @Operation(summary = "查询文件目录树", description = "权限接口。查询当前机构下文件中心逻辑目录树")
    public R<List<FileDirectoryVO>> tree() {
        return R.ok(directoryService.tree());
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:directories:add")
    @Operation(summary = "新增文件目录", description = "权限接口。新增当前机构下文件逻辑目录")
    public R<Long> create(
            @RequestBody SaveFileDirectoryCommand command) {
        return R.ok(directoryService.create(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:directories:edit")
    @Operation(summary = "修改文件目录", description = "权限接口。修改文件逻辑目录名称、排序或状态")
    public R<Boolean> update(
            @RequestBody SaveFileDirectoryCommand command) {
        return R.ok(directoryService.update(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:directories:delete")
    @Operation(summary = "删除文件目录", description = "权限接口。只能删除空目录")
    public R<Boolean> delete(
            @Parameter(description = "目录ID", required = true)
            @RequestParam("id") Long id) {
        return R.ok(directoryService.delete(id));
    }
}
