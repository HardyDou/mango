package io.mango.resource.starter.remote;

import io.mango.common.result.R;
import io.mango.resource.api.ResourceRegistryApi;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 资源注册中心远程调用适配。
 */
@FeignClient(name = "mango-resource", contextId = "resourceRegistryFeignClient")
public interface ResourceRegistryFeignClient extends ResourceRegistryApi {

    @Override
    @PostMapping("/resource/declarations/register")
    R<Boolean> registerDeclarations(@RequestBody RegisterResourceDeclarationsCommand command);
}
