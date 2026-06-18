package io.mango.authorization.resource.sync;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import io.mango.infra.module.api.ModuleInfoRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * 扫描 Spring MVC 映射并注册 API 资源。
 */
@Slf4j
public class ApiResourceSyncRunner implements ApplicationRunner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApiResourceApi apiResourceApi;
    private final ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider;
    private final ApiResourceSyncProperties properties;

    public ApiResourceSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            ApiResourceApi apiResourceApi) {
        this(handlerMapping, apiResourceApi, new EmptyModuleInfoRegistryProvider(), new ApiResourceSyncProperties());
    }

    public ApiResourceSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            ApiResourceApi apiResourceApi,
            ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider) {
        this(handlerMapping, apiResourceApi, moduleInfoRegistryProvider, new ApiResourceSyncProperties());
    }

    public ApiResourceSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            ApiResourceApi apiResourceApi,
            ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider,
            ApiResourceSyncProperties properties) {
        this.handlerMapping = handlerMapping;
        this.apiResourceApi = apiResourceApi;
        this.moduleInfoRegistryProvider = moduleInfoRegistryProvider;
        this.properties = properties == null ? new ApiResourceSyncProperties() : properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ApiResourceRegisterCommand> resources = scanResources();
        if (resources.isEmpty()) {
            log.info("API resource sync skipped: no resources discovered");
            return;
        }
        if ("read".equalsIgnoreCase(properties.getMode())) {
            log.info("API resource sync read-only: discovered {} resources", resources.size());
            return;
        }
        R<ApiResourceRegisterResultVO> response = apiResourceApi.registerApiResources(resources);
        ApiResourceRegisterResultVO result = response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : ApiResourceRegisterResultVO.empty();
        log.info("API resource sync complete: scanned={}, created={}, updated={}",
                result.scanned(), result.created(), result.updated());
    }

    List<ApiResourceRegisterCommand> scanResources() {
        return new ApiAccessResourceDiscoverer(handlerMapping, moduleInfoRegistryProvider, properties).discover();
    }

    private static class EmptyModuleInfoRegistryProvider implements ObjectProvider<ModuleInfoRegistry> {

        @Override
        public ModuleInfoRegistry getObject(Object... args) {
            return null;
        }

        @Override
        public ModuleInfoRegistry getIfAvailable() {
            return null;
        }

        @Override
        public ModuleInfoRegistry getIfUnique() {
            return null;
        }

        @Override
        public ModuleInfoRegistry getObject() {
            return null;
        }
    }
}
