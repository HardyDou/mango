package io.mango.infra.realtime.starter;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

/**
 * 注册非 Spring MVC 扫描链路中的 WebSocket 实时传输资源。
 */
@RequiredArgsConstructor
public class RealtimeWebSocketResourceRegistrar implements ApplicationRunner {

    static final String MODULE_NAME = "mango-infra-realtime";
    static final String HANDLER_CLASS = "io.mango.infra.realtime.core.websocket.RealtimeWebSocketConfiguration";
    static final String WEBSOCKET_PROBE_ENDPOINT = "/realtime/transports/probe/websocket";

    private final ApiResourceApi apiResourceApi;
    private final MangoRealtimeProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        apiResourceApi.registerApiResources(List.of(
                loginGet(properties.getWebsocket().getEndpoint(), "建立 WebSocket 实时连接"),
                loginGet(WEBSOCKET_PROBE_ENDPOINT, "探测 WebSocket 链路")
        ));
    }

    private static ApiResourceRegisterCommand loginGet(String pathPattern, String description) {
        ApiResourceRegisterCommand command = new ApiResourceRegisterCommand();
        command.setModuleName(MODULE_NAME);
        command.setHttpMethod("GET");
        command.setPathPattern(pathPattern);
        command.setResourceCode("GET:" + pathPattern);
        command.setAccessMode(ApiResourceAccessMode.LOGIN);
        command.setHandlerClass(HANDLER_CLASS);
        command.setHandlerMethod("websocket");
        command.setDescription(description);
        return command;
    }
}
