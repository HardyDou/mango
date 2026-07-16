package io.mango.resource.core.service;

import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;

/**
 * 资源声明注册与同步服务。
 */
public interface IResourceRegistryService {

    void sync();

    void sync(boolean force);

    Boolean registerDeclarations(RegisterResourceDeclarationsCommand command);

    void deleteResource(String resourceId, boolean physical);
}
