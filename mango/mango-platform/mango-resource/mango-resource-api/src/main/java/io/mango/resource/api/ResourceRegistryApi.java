package io.mango.resource.api;

import io.mango.common.result.R;
import io.mango.infra.web.api.Inner;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

/**
 * 资源注册中心 API 契约。
 */
@Validated
public interface ResourceRegistryApi {

    /**
     * 注册远程服务上报的资源声明。
     *
     * @param command 资源声明注册命令。
     * @return true 表示注册成功。
     */
    @Inner
    R<Boolean> registerDeclarations(@Valid RegisterResourceDeclarationsCommand command);
}
