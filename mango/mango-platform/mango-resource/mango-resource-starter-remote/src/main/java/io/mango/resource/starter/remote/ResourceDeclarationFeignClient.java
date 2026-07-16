package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.resource.api.ResourceDeclarationApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 资源声明注册远程调用适配。
 */
@FeignClient(name = "mango-resource", contextId = "resourceDeclarationFeignClient", path = "/resource")
public interface ResourceDeclarationFeignClient extends ResourceDeclarationApi {

    @Override
    @PostMapping("/declarations/register")
    R<Boolean> registerDeclarations(@RequestBody RegisterResourceDeclarationsCommand command);
}
