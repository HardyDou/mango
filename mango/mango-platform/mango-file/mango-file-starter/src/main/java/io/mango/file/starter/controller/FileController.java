package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.FilePackageSizeControlCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FilePackageResultVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** File metadata and lifecycle endpoints. */
@RestController("mangoFileController")
@RequestMapping("/file/files")
@RequiredArgsConstructor
@Validated
@Tag(name = "文件管理", description = "文件记录查询、派生、归档与删除接口")
public class FileController implements FileApi {

    private final IFileService fileService;

    @Override
    @GetMapping("/page")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:list")
    @Operation(summary = "分页查询文件记录", description = "按当前登录机构分页查询文件记录")
    public R<PageResult<FileRecordVO>> page(@ParameterObject FileRecordPageQuery query) {
        return R.ok(fileService.page(query));
    }

    @Override
    @GetMapping("/detail")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "获取文件详情", description = "按文件ID查询当前租户可见文件记录详情")
    public R<FileRecordVO> get(
            @Parameter(description = "文件ID", required = true) @RequestParam("id") Long id) {
        return R.ok(fileService.get(id));
    }

    @Override
    @GetMapping("/preview")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "获取文件预览元数据", description = "返回当前租户可见文件的预览与下载元数据")
    public R<FilePreviewVO> preview(
            @Parameter(description = "文件ID", required = true) @RequestParam("id") Long id) {
        return R.ok(fileService.preview(id));
    }

    @Override
    @PostMapping("/package")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "打包文件", description = "按当前租户可见文件清单生成并保存ZIP文件")
    public R<FileRecordVO> packageFiles(@RequestBody FilePackageCommand command) {
        return R.ok(fileService.packageFiles(command));
    }

    @Override
    @PostMapping("/package-size-control")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "按目标大小打包文件", description = "生成单个ZIP，并返回自动或手动大小控制结果")
    public R<FilePackageResultVO> packageFilesWithSizeControl(
            @RequestBody FilePackageSizeControlCommand command) {
        return R.ok(fileService.packageFilesWithSizeControl(command));
    }

    @Override
    @PostMapping("/merge-pdf")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "合并生成PDF", description = "按当前租户可见文件清单生成并保存PDF文件")
    public R<FileRecordVO> mergeToPdf(@RequestBody FileMergePdfCommand command) {
        return R.ok(fileService.mergeToPdf(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:archive")
    @Operation(summary = "归档文件", description = "归档文件记录且默认不物理删除存储对象")
    public R<Boolean> archive(
            @Parameter(description = "文件ID", required = true) @RequestParam("id") Long id,
            @Parameter(description = "归档原因") @RequestParam(value = "reason", required = false) String reason) {
        FileArchiveCommand command = new FileArchiveCommand();
        command.setId(id);
        command.setReason(reason);
        return R.ok(fileService.archive(command));
    }

    @Override
    @PostMapping("/delete")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:files:delete")
    @Operation(summary = "删除文件", description = "按物理删除策略删除一个或多个文件记录")
    public R<Boolean> delete(@RequestBody FileDeleteCommand command) {
        return R.ok(fileService.delete(command));
    }
}
