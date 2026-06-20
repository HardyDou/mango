package io.mango.authorization.resource.sync.gateway;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

/**
 * Legacy direct writer for Spring Cloud Gateway route API resources.
 */
@Slf4j
public class GatewayRouteResourceSyncRunner implements ApplicationRunner {

    private final GatewayRouteResourceDiscoverer discoverer;
    private final ApiResourceApi apiResourceApi;
    private final String syncMode;

    public GatewayRouteResourceSyncRunner(
            GatewayRouteResourceDiscoverer discoverer,
            ApiResourceApi apiResourceApi,
            String syncMode) {
        this.discoverer = discoverer;
        this.apiResourceApi = apiResourceApi;
        this.syncMode = syncMode;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ApiResourceRegisterCommand> resources = scanRoutes();
        if (resources.isEmpty()) {
            log.info("Gateway route resource sync skipped: no Path routes discovered");
            return;
        }
        if ("read".equalsIgnoreCase(syncMode)) {
            log.info("Gateway route resource sync read-only: discovered {} resources", resources.size());
            return;
        }
        R<ApiResourceRegisterResultVO> response = apiResourceApi.registerApiResources(resources);
        ApiResourceRegisterResultVO result = response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : ApiResourceRegisterResultVO.empty();
        log.info("Gateway route resource sync complete: scanned={}, created={}, updated={}",
                result.scanned(), result.created(), result.updated());
    }

    List<ApiResourceRegisterCommand> scanRoutes() {
        return discoverer.discover();
    }
}
