package io.mango.ai.starter.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.ai.api.AiModelManagementApi;
import io.mango.ai.api.command.CreateAiModelCommand;
import io.mango.ai.api.command.CreateAiProviderConnectionCommand;
import io.mango.ai.api.command.SetAiCapabilityRouteCommand;
import io.mango.ai.api.command.UpdateAiModelCommand;
import io.mango.ai.api.command.UpdateAiProviderConnectionCommand;
import io.mango.ai.api.vo.AiCapabilityRouteVO;
import io.mango.ai.api.vo.AiModelVO;
import io.mango.ai.api.vo.AiProviderConnectionVO;
import io.mango.ai.api.vo.AiProviderTypeVO;
import io.mango.ai.core.service.IAiModelManagementService;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

/** AI 模型管理 HTTP 适配器。 */
@RestController
@Validated
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring injects the application service; copying a container-managed collaborator is not valid"))
@RequestMapping("/ai/models")
public class AiModelManagementController implements AiModelManagementApi {
    private final IAiModelManagementService service;

    @Override
    @GetMapping("/providers")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiProviderConnectionVO>> providers() {
        return R.ok(service.providers());
    }

    @Override
    @GetMapping("/provider-types")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiProviderTypeVO>> providerTypes() {
        return R.ok(service.providerTypes());
    }

    @Override
    @GetMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiModelVO>> models(
            @RequestParam Long providerConnectionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return R.ok(service.models(providerConnectionId, keyword, enabled));
    }

    @Override
    @GetMapping("/routes")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:list")
    public R<List<AiCapabilityRouteVO>> routes() {
        return R.ok(service.routes());
    }

    @Override
    @PostMapping("/providers")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:provider:add")
    public R<Long> createProvider(@Valid @RequestBody CreateAiProviderConnectionCommand command) {
        return R.ok(service.createProvider(command));
    }

    @Override
    @PutMapping("/providers")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:provider:edit")
    public R<Boolean> updateProvider(@Valid @RequestBody UpdateAiProviderConnectionCommand command) {
        return R.ok(service.updateProvider(command));
    }

    @Override
    @DeleteMapping("/providers")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:provider:delete")
    public R<Boolean> deleteProvider(@NotNull @Positive @RequestParam Long id) {
        return R.ok(service.deleteProvider(id));
    }

    @Override
    @PostMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:add")
    public R<Long> createModel(@Valid @RequestBody CreateAiModelCommand command) {
        return R.ok(service.createModel(command));
    }

    @Override
    @PutMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:edit")
    public R<Boolean> updateModel(@Valid @RequestBody UpdateAiModelCommand command) {
        return R.ok(service.updateModel(command));
    }

    @Override
    @DeleteMapping
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:delete")
    public R<Boolean> deleteModel(@NotNull @Positive @RequestParam Long id) {
        return R.ok(service.deleteModel(id));
    }

    @Override
    @PutMapping("/routes")
    @ApiAccess(mode = ApiResourceAccessMode.PERMISSION, permission = "ai:model:route:edit")
    public R<Boolean> setRoute(@Valid @RequestBody SetAiCapabilityRouteCommand command) {
        return R.ok(service.setRoute(command));
    }
}
