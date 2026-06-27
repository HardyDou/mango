package io.mango.file.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.mango.file.api.command.SaveFileSettingsCommand;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.service.IFileSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件中心运行时配置接口。
 */
@RestController
@RequestMapping("/file/settings")
@RequiredArgsConstructor
@Tag(name = "文件中心配置", description = "上传策略、访问策略、预览策略和直传策略配置接口")
public class FileSettingsController {

    private final IFileSettingsService settingsService;

    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.LOGIN, desc = "获取文件中心配置")
    @Operation(summary = "获取文件中心配置", description = "登录接口。返回当前机构文件中心运行时配置，未保存时返回 yml 默认值")
    public R<FileSettingsVO> get() {
        return settingsService.get();
    }

    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "file:settings:edit")
    @Operation(summary = "保存文件中心配置", description = "权限接口。保存上传类型、大小、秒传、直传、访问有效期和预览服务配置")
    public R<Boolean> save(
            @Parameter(description = "文件中心运行时配置", required = true)
            @Valid @RequestBody SaveFileSettingsCommand command) {
        return settingsService.save(command);
    }
}
