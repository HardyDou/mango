package io.mango.integration.starter.controller;

import io.mango.integration.api.IntegrationCredentialApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.integration.api.command.IntegrationCredentialCommand;
import io.mango.integration.api.command.IntegrationCredentialRotateCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.integration.api.vo.IntegrationCredentialRotationVO;
import io.mango.integration.api.vo.IntegrationCredentialVO;
import io.mango.integration.core.service.IIntegrationCredentialService;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 集成凭证控制器。
 */
@RestController
@RequestMapping("/integration/credentials")
@RequiredArgsConstructor
@Validated
@Tag(name = "集成凭证", description = "集成凭证管理接口，敏感字段只写入不读取")
public class IntegrationCredentialController implements IntegrationCredentialApi {

    private final IIntegrationCredentialService service;

    @Override
    @GetMapping
    @Operation(summary = "查询凭证列表", description = "只返回非敏感字段与敏感字段掩码")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:credential:list")
    public R<List<IntegrationCredentialVO>> list(
            @Parameter(description = "连接器编码")
            @RequestParam(name = "connectorCode", required = false) String connectorCode) {
        return R.ok(service.list(connectorCode));
    }

    @Override
    @PostMapping
    @Operation(summary = "保存凭证", description = "按连接器认证规范校验，敏感字段加密保存")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:credential:add")
    public R<Boolean> save(@RequestBody IntegrationCredentialCommand command) {
        return R.ok(service.save(command));
    }

    @Override
    @GetMapping("/rotations")
    @Operation(summary = "查询凭证轮换记录", description = "按凭证编码倒序返回轮换历史，不含凭证原文")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:credential:list")
    public R<List<IntegrationCredentialRotationVO>> rotations(
            @Parameter(description = "凭证编码")
            @RequestParam(name = "credentialCode") String credentialCode) {
        return R.ok(service.rotations(credentialCode));
    }

    @Override
    @PostMapping("/rotate")
    @Operation(summary = "轮换凭证", description = "只更新提交的字段，未提交字段保持原值并递增版本")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "integration:credential:rotate")
    public R<Boolean> rotate(@RequestBody IntegrationCredentialRotateCommand command) {
        return R.ok(service.rotate(command));
    }
}
