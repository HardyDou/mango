package io.mango.ai.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.ai.api.AiModelManagementApi;
import io.mango.ai.api.command.CreateAiModelCommand;
import io.mango.ai.api.command.CreateAiProviderConnectionCommand;
import io.mango.ai.api.command.SetAiCapabilityRouteCommand;
import io.mango.ai.api.command.UpdateAiModelCommand;
import io.mango.ai.api.command.UpdateAiProviderConnectionCommand;
import io.mango.ai.api.query.AiModelQuery;
import io.mango.ai.api.vo.AiCapabilityRouteVO;
import io.mango.ai.api.vo.AiModelVO;
import io.mango.ai.api.vo.AiProviderConnectionVO;
import io.mango.ai.api.vo.AiProviderTypeVO;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** AI 模型管理 HTTP 适配器。 */
@RestController
@Validated
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects the application service; copying a container-managed collaborator is not valid"))
@RequestMapping("/ai/models")
@Tag(name = "AI 模型管理", description = "管理 AI 供应商连接、模型目录和能力路由")
public class AiModelManagementController implements AiModelManagementApi {
    private final IAiModelManagementService service;

    @Override
    @GetMapping("/providers")
    @Operation(summary = "查询供应商连接", description = "查询当前租户已配置的 AI 供应商连接")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiProviderConnectionVO>> providers() {
        return R.ok(service.providers());
    }

    @Override
    @GetMapping("/provider-types")
    @Operation(summary = "查询供应商类型", description = "查询系统内置的 AI 供应商类型及默认接入信息")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiProviderTypeVO>> providerTypes() {
        return R.ok(service.providerTypes());
    }

    @Override
    @GetMapping
    @Operation(summary = "查询模型", description = "按供应商、关键词和启用状态查询模型目录")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiModelVO>> models(@ParameterObject AiModelQuery query) {
        return R.ok(service.models(query));
    }

    @Override
    @GetMapping("/routes")
    @Operation(summary = "查询能力路由", description = "查询各 AI 能力当前使用的默认模型")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiCapabilityRouteVO>> routes() {
        return R.ok(service.routes());
    }

    @Override
    @PostMapping("/providers")
    @Operation(summary = "新增供应商连接", description = "新增一个 AI 供应商连接")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:provider:add")
    public R<Long> createProvider(@RequestBody CreateAiProviderConnectionCommand command) {
        return R.ok(service.createProvider(command));
    }

    @Override
    @PutMapping("/providers")
    @Operation(summary = "修改供应商连接", description = "修改一个 AI 供应商连接")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:provider:edit")
    public R<Boolean> updateProvider(@RequestBody UpdateAiProviderConnectionCommand command) {
        return R.ok(service.updateProvider(command));
    }

    @Override
    @DeleteMapping("/providers")
    @Operation(summary = "删除供应商连接", description = "删除没有关联模型的 AI 供应商连接")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:provider:delete")
    public R<Boolean> deleteProvider(
            @Parameter(description = "供应商连接标识") @RequestParam("id") Long id) {
        return R.ok(service.deleteProvider(id));
    }

    @Override
    @PostMapping
    @Operation(summary = "新增模型", description = "在指定供应商连接下新增模型")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:add")
    public R<Long> createModel(@RequestBody CreateAiModelCommand command) {
        return R.ok(service.createModel(command));
    }

    @Override
    @PutMapping
    @Operation(summary = "修改模型", description = "修改模型名称、能力、模态和启用状态")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:edit")
    public R<Boolean> updateModel(@RequestBody UpdateAiModelCommand command) {
        return R.ok(service.updateModel(command));
    }

    @Override
    @DeleteMapping
    @Operation(summary = "删除模型", description = "删除未被能力路由引用的模型")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:delete")
    public R<Boolean> deleteModel(@Parameter(description = "模型标识") @RequestParam("id") Long id) {
        return R.ok(service.deleteModel(id));
    }

    @Override
    @PutMapping("/routes")
    @Operation(summary = "设置能力路由", description = "为指定 AI 能力设置默认模型")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:route:edit")
    public R<Boolean> setRoute(@RequestBody SetAiCapabilityRouteCommand command) {
        return R.ok(service.setRoute(command));
    }
}
