package io.mango.access.starter.gateway.filter;

import io.mango.access.api.vo.AccessContextValidationResultVO;
import io.mango.access.core.auth.AccessEvaluator;
import io.mango.access.core.config.AccessProperties;
import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ITokenProvider;
import io.mango.authorization.api.command.ApiResourceRegisterRequest;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.query.ApiResourceAccessDecisionQuery;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.vo.TokenPairVO;
import io.mango.common.result.R;
import io.mango.infra.context.api.MangoContextHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthGlobalFilterTest {

    private final TestApiResourceApi apiResourceApi = new TestApiResourceApi();
    private final AccessEvaluator accessEvaluator = new AccessEvaluator(
            new AccessProperties(), new TestTokenProvider(), apiResourceApi, new TestAuthorizationProvider());

    @Test
    @DisplayName("PUBLIC 请求转发前应删除外部注入的安全身份头")
    void filter_shouldRemoveUntrustedSecurityHeadersWhenPublic() {
        apiResourceApi.accessMode = ApiResourceAccessMode.PUBLIC;
        MockServerHttpRequest request = MockServerHttpRequest.get("/public/context")
                .header(MangoContextHeaders.USER_ID, "999")
                .header(MangoContextHeaders.TENANT_ID, "spoofed-tenant")
                .header(MangoContextHeaders.APP_CODE, "spoofed-app")
                .build();
        CapturingChain chain = new CapturingChain();

        new AuthGlobalFilter(() -> accessEvaluator)
                .filter(MockServerWebExchange.from(request), chain)
                .block();

        assertNull(chain.request.getHeaders().getFirst(MangoContextHeaders.USER_ID));
        assertNull(chain.request.getHeaders().getFirst(MangoContextHeaders.TENANT_ID));
        assertNull(chain.request.getHeaders().getFirst(MangoContextHeaders.APP_CODE));
    }

    @Test
    @DisplayName("Realtime probe ticket 请求应到达下游并清除外部身份头")
    void filter_shouldPassRealtimeProbeTicketWithoutTrustingIdentityHeaders() {
        apiResourceApi.accessMode = ApiResourceAccessMode.LOGIN;
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/realtime/transports/probe/websocket?rtTicket=issued-ticket")
                .header(MangoContextHeaders.USER_ID, "999")
                .build();
        CapturingChain chain = new CapturingChain();

        new AuthGlobalFilter(() -> accessEvaluator)
                .filter(MockServerWebExchange.from(request), chain)
                .block();

        assertEquals("/realtime/transports/probe/websocket", chain.request.getURI().getPath());
        assertNull(chain.request.getHeaders().getFirst(MangoContextHeaders.USER_ID));
        assertEquals(0, apiResourceApi.resolveCount);
    }

    @Test
    @DisplayName("错误消息应转义为合法 JSON")
    void filter_shouldEscapeErrorMessageAsJson() {
        AccessEvaluator evaluator = new AccessEvaluator(
                new AccessProperties(), new TestTokenProvider(), apiResourceApi,
                new TestAuthorizationProvider(),
                List.of(principal -> AccessContextValidationResultVO.deny("invalid \"tenant\"\ncontext")));
        MockServerHttpRequest request = MockServerHttpRequest.get("/profile")
                .header("Authorization", "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        new AuthGlobalFilter(() -> evaluator)
                .filter(exchange, ignored -> Mono.empty())
                .block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals("{\"code\":401,\"message\":\"invalid \\\"tenant\\\"\\ncontext\"}",
                exchange.getResponse().getBodyAsString().block());
    }

    private static final class CapturingChain implements GatewayFilterChain {

        private ServerHttpRequest request;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            request = exchange.getRequest();
            return Mono.empty();
        }
    }

    private static final class TestApiResourceApi implements ApiResourceApi {

        private ApiResourceAccessMode accessMode = ApiResourceAccessMode.LOGIN;
        private int resolveCount;

        @Override
        public R<ApiResourceRegisterResultVO> registerApiResources(ApiResourceRegisterRequest request) {
            return R.ok(ApiResourceRegisterResultVO.empty());
        }

        @Override
        public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            resolveCount++;
            return R.ok(new ApiResourceAccessDecisionVO(true, accessMode, null));
        }

        @Override
        public R<Void> refreshApiResourceCache() {
            return R.ok();
        }
    }

    private static final class TestTokenProvider implements ITokenProvider {

        @Override
        public String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims) {
            return "valid-token";
        }

        @Override
        public String generateRefreshToken(Long userId, String username) {
            return "refresh-token";
        }

        @Override
        public boolean validateToken(String token) {
            return "valid-token".equals(token);
        }

        @Override
        public Long getUserId(String token) {
            return 1L;
        }

        @Override
        public String getUsername(String token) {
            return "test-user";
        }

        @Override
        public String getTokenType(String token) {
            return TOKEN_TYPE_ACCESS;
        }

        @Override
        public String getClaim(String token, String claimName) {
            return null;
        }

        @Override
        public TokenPairVO refresh(String refreshToken) {
            return new TokenPairVO("valid-token", "refresh-token");
        }
    }

    private static final class TestAuthorizationProvider implements IAuthorizationProvider {

        @Override
        public AuthorizationSnapshotVO load(AuthorizationQuery query) {
            return AuthorizationSnapshotVO.of(List.of(), List.of("*:*"), List.of("*:*"));
        }
    }
}
