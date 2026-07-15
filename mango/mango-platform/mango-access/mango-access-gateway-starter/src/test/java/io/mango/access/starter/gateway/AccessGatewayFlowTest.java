package io.mango.access.starter.gateway;

import io.mango.authorization.api.ApiResourceApi;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Tag("flow")
@Tag("access")
@SpringBootTest(classes = AccessGatewayFlowTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessGatewayFlowTest {

    private static final DisposableServer DOWNSTREAM = HttpServer.create().port(0)
            .handle((request, response) -> response.sendString(reactor.core.publisher.Mono.just(
                    value(request.requestHeaders().get(MangoContextHeaders.USER_ID)) + "|"
                            + value(request.requestHeaders().get(MangoContextHeaders.TENANT_ID)) + "|"
                            + value(request.requestHeaders().get(MangoContextHeaders.APP_CODE)) + "|"
                            + request.uri())))
            .bindNow();

    @LocalServerPort
    private int port;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .responseTimeout(Duration.ofSeconds(10))
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.disposeNow();
    }

    @Test
    void publicRequest_shouldStripSpoofedHeadersAcrossRealGatewayRoute() {
        client.get().uri("/public/context")
                .header(MangoContextHeaders.USER_ID, "999")
                .header(MangoContextHeaders.TENANT_ID, "spoofed-tenant")
                .header(MangoContextHeaders.APP_CODE, "spoofed-app")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("|||/public/context");
    }

    @Test
    void authenticatedRequest_shouldReplaceIdentityAcrossRealGatewayRoute() {
        client.get().uri("/secure/context")
                .headers(headers -> headers.setBearerAuth("valid"))
                .header(MangoContextHeaders.USER_ID, "999")
                .header(MangoContextHeaders.TENANT_ID, "spoofed-tenant")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("101|trusted-tenant|trusted-app|/secure/context");
    }

    @Test
    void realtimeTicket_shouldReachDownstreamButNeverTrustIdentityHeaders() {
        client.get().uri("/realtime/transports/probe/websocket?rtTicket=issued-ticket")
                .header(MangoContextHeaders.USER_ID, "999")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("|||/realtime/transports/probe/websocket?rtTicket=issued-ticket");
    }

    @Test
    void policyFailure_shouldReturn503AtRealGatewayEntry() {
        client.get().uri("/unavailable").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(String.class)
                .isEqualTo("{\"code\":503,\"message\":\"访问策略服务暂不可用\"}");
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestBeans.class)
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean ApiResourceApi apiResourceApi() { return new FlowResourceApi(); }
        @Bean ITokenProvider tokenProvider() { return new FlowTokenProvider(); }
        @Bean IAuthorizationProvider authorizationProvider() {
            return query -> AuthorizationSnapshotVO.of(List.of(), List.of("*:*"), List.of());
        }
        @Bean RouteLocator flowRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("flow-downstream", route -> route.path("/**")
                            .uri("http://127.0.0.1:" + DOWNSTREAM.port()))
                    .build();
        }
    }

    private static final class FlowResourceApi implements ApiResourceApi {
        @Override public R<ApiResourceRegisterResultVO> registerApiResources(ApiResourceRegisterRequest request) {
            return R.ok(ApiResourceRegisterResultVO.empty());
        }
        @Override public R<ApiResourceAccessDecisionVO> resolveAccessDecision(ApiResourceAccessDecisionQuery query) {
            if ("/unavailable".equals(query.getPath())) return R.fail("offline");
            ApiResourceAccessMode mode = query.getPath().startsWith("/public/")
                    ? ApiResourceAccessMode.PUBLIC : ApiResourceAccessMode.LOGIN;
            return R.ok(new ApiResourceAccessDecisionVO(true, mode, null));
        }
        @Override public R<Void> refreshApiResourceCache() { return R.ok(); }
    }

    private static final class FlowTokenProvider implements ITokenProvider {
        private final Map<String, String> claims = Map.of(
                "memberId", "201", "tenantId", "trusted-tenant", "realm", "INTERNAL",
                "actorType", "USER", "partyType", "COMPANY", "partyId", "301", "appCode", "trusted-app");
        @Override public String generateAccessToken(Long userId, String username, Map<String, Object> claims) { return "valid"; }
        @Override public String generateRefreshToken(Long userId, String username) { return "refresh"; }
        @Override public boolean validateToken(String token) { return "valid".equals(token); }
        @Override public Long getUserId(String token) { return 101L; }
        @Override public String getUsername(String token) { return "admin"; }
        @Override public String getTokenType(String token) { return TOKEN_TYPE_ACCESS; }
        @Override public String getClaim(String token, String name) { return claims.get(name); }
        @Override public TokenPairVO refresh(String token) { return new TokenPairVO("valid", "refresh"); }
    }
}
