package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.file.api.FileImportApi;
import io.mango.file.api.command.ImportRemoteImageCommand;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.service.IRemoteFileImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlled remote file import endpoint. */
@RestController("mangoFileImportController")
@RequestMapping("/file/files")
@RequiredArgsConstructor
@Validated
@Tag(name = "文件导入", description = "受控远程图片导入接口")
public class FileImportController implements FileImportApi {

    private final IRemoteFileImportService remoteFileImportService;

    @Override
    @PostMapping("/import-image")
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN)
    @Operation(summary = "导入远程图片", description = "安全获取公网图片并保存到当前租户文件中心")
    public R<FileRecordVO> importImage(@Valid @RequestBody ImportRemoteImageCommand command) {
        return R.ok(remoteFileImportService.importImage(command));
    }
}
