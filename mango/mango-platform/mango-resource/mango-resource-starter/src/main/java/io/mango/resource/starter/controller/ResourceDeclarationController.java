package io.mango.resource.starter.controller;

import io.mango.common.result.R;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.core.service.IResourceRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源声明注册入口。
 */
@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
@Validated
@Tag(name = "资源声明注册", description = "内部服务上报资源声明的注册接口")
public class ResourceDeclarationController implements ResourceDeclarationApi {

    private final IResourceRegistryService resourceRegistryService;

    @Override
    @PostMapping("/declarations/register")
    @Operation(summary = "注册远程资源声明", description = "接收内部服务上报的资源声明并执行注册同步")
    public R<Boolean> registerDeclarations(
            @RequestBody RegisterResourceDeclarationsCommand command) {
        return R.ok(resourceRegistryService.registerDeclarations(command));
    }
}
