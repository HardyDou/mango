package io.mango.access.starter.web;

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
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextHeaders;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("flow")
@Tag("access")
@SpringBootTest(classes = AccessWebFlowTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessWebFlowTest {

    private final TestRestTemplate restTemplate;

    @Autowired
    AccessWebFlowTest(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Test
    void publicRequest_shouldReachControllerWithoutSpoofedSecurityContext() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(MangoContextHeaders.REQUEST_ID, "request-flow-1");
        headers.set(MangoContextHeaders.TENANT_ID, "spoofed-tenant");
        headers.set(MangoContextHeaders.APP_CODE, "spoofed-app");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/public/context", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("request-flow-1", response.getBody().get("requestId"));
        assertFalse(response.getBody().containsKey("tenantId"));
        assertFalse(response.getBody().containsKey("appCode"));
    }

    @Test
    void authenticatedRequest_shouldReachControllerWithOnlyValidatedPrincipal() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("valid");
        headers.set(MangoContextHeaders.TENANT_ID, "spoofed-tenant");
        headers.set(MangoContextHeaders.APP_CODE, "spoofed-app");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/secure/context", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("101", response.getBody().get("userId"));
        assertEquals("trusted-tenant", response.getBody().get("tenantId"));
        assertEquals("trusted-app", response.getBody().get("appCode"));
    }

    @Test
    void policyFailure_shouldReturn503BeforeController() {
        ResponseEntity<String> response = restTemplate.getForEntity("/unavailable", String.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("{\"code\":503,\"message\":\"访问策略服务暂不可用\"}", response.getBody());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({TestBeans.class, ContextController.class})
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean ApiResourceApi apiResourceApi() { return new FlowResourceApi(); }
        @Bean ITokenProvider tokenProvider() { return new FlowTokenProvider(); }
        @Bean IAuthorizationProvider authorizationProvider() {
            return query -> AuthorizationSnapshotVO.of(List.of(), List.of("*:*"), List.of());
        }
    }

    @RestController
    static class ContextController {
        @GetMapping({"/public/context", "/secure/context"})
        Map<String, Object> context() {
            java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
            put(result, "requestId", MangoContextHolder.requestId());
            put(result, "userId", MangoContextHolder.userId());
            put(result, "tenantId", MangoContextHolder.tenantId());
            put(result, "appCode", MangoContextHolder.appCode());
            return result;
        }

        private void put(Map<String, Object> result, String key, Object value) {
            if (value != null) result.put(key, value);
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
