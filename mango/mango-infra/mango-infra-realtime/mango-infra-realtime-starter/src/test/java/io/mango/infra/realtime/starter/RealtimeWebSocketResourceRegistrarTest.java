package io.mango.infra.realtime.starter;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeWebSocketResourceRegistrarTest {

    @Test
    void runShouldRegisterConfiguredWebSocketEndpointsAsLoginResources() {
        CapturingApiResourceApi api = new CapturingApiResourceApi();
        MangoRealtimeProperties properties = new MangoRealtimeProperties();
        properties.getWebsocket().setEndpoint("/custom/ws");

        new RealtimeWebSocketResourceRegistrar(api, properties).run(null);

        assertThat(api.resources).hasSize(2);
        assertThat(api.resources)
                .extracting(ApiResourceRegisterCommand::getPathPattern)
                .containsExactly("/custom/ws", "/realtime/transports/probe/websocket");
        assertThat(api.resources)
                .allSatisfy(resource -> {
                    assertThat(resource.getModuleName()).isEqualTo(RealtimeWebSocketResourceRegistrar.MODULE_NAME);
                    assertThat(resource.getHttpMethod()).isEqualTo("GET");
                    assertThat(resource.getAccessMode()).isEqualTo(ApiResourceAccessMode.LOGIN);
                    assertThat(resource.getPermissionCode()).isNull();
                });
    }

    private static final class CapturingApiResourceApi implements ApiResourceApi {

        private final List<ApiResourceRegisterCommand> resources = new ArrayList<>();

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(List<ApiResourceRegisterCommand> resources) {
            this.resources.addAll(resources);
            return R.ok(ApiResourceRegisterResultVO.empty());
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            return R.ok(ApiResourceAccessDecisionVO.unmatched(ApiResourceAccessMode.LOGIN));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
        }
    }
}
