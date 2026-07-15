package io.mango.infra.doc.starter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.annotation.InternalAccess;
import io.mango.authorization.api.annotation.PublicAccess;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = DocOpenApiFlowTest.FlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.doc.title=Infra Doc Flow API",
                "mango.doc.contact.name=Infra Doc Flow Team",
                "mango.doc.module-grouping.include-default-group=false",
                "mango.module.module-service.modules.mango-infra-doc-flow.module-path=/doc-flow/,/doc-alias/"
        })
@org.junit.jupiter.api.Tag("flow")
@org.junit.jupiter.api.Tag("infra-doc")
class DocOpenApiFlowTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void groupedOpenApiEndpointShouldUseConfiguredNormalizedModulePath() throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(
                                "http://127.0.0.1:" + port + "/v3/api-docs/mango-infra-doc-flow"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        JsonNode document = objectMapper.readTree(response.body());
        assertEquals("Infra Doc Flow API", document.path("info").path("title").asText());
        assertEquals("Infra Doc Flow Team", document.path("info").path("contact").path("name").asText());
        assertTrue(document.path("paths").has("/doc-flow/ping"));
        assertTrue(document.path("paths").has("/doc-flow/internal"));
        assertTrue(document.path("paths").has("/doc-alias/ping"));
        assertFalse(document.path("paths").has("/stale-doc-flow/ping"));
        JsonNode publicOperation = document.path("paths").path("/doc-flow/ping").path("get");
        assertEquals(MangoApiScopeOperationCustomizer.EXTERNAL_SCOPE,
                publicOperation.path(MangoApiScopeOperationCustomizer.SCOPE_EXTENSION).asText());
        assertTrue(publicOperation.path("security").isArray());
        assertEquals(0, publicOperation.path("security").size());

        JsonNode internalOperation = document.path("paths").path("/doc-flow/internal").path("get");
        assertEquals(MangoApiScopeOperationCustomizer.INTERNAL_SCOPE,
                internalOperation.path(MangoApiScopeOperationCustomizer.SCOPE_EXTENSION).asText());
        assertTrue(internalOperation.path("tags").toString().contains("对内接口"));
        assertTrue(internalOperation.path("security").toString()
                .contains(MangoApiScopeOperationCustomizer.AUTHORIZATION_HEADER_SCHEME));
        assertTrue(document.path("components").path("securitySchemes")
                .has(MangoApiScopeOperationCustomizer.AUTHORIZATION_HEADER_SCHEME));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({FlowController.class, AliasFlowController.class})
    static class FlowApplication {
    }

    @RestController
    @RequestMapping("/doc-flow")
    @io.swagger.v3.oas.annotations.tags.Tag(name = "文档流程", description = "验证模块化 OpenAPI 文档入口")
    static class FlowController {

        @GetMapping("/ping")
        @PublicAccess(desc = "文档入口健康检查")
        @io.swagger.v3.oas.annotations.Operation(
                summary = "检查文档入口",
                description = "通过真实 HTTP OpenAPI 文档确认模块分组、路径与访问范围")
        String ping() {
            return "pong";
        }

        @GetMapping("/internal")
        @InternalAccess(desc = "内部文档入口健康检查")
        @io.swagger.v3.oas.annotations.Operation(
                summary = "检查内部文档入口",
                description = "确认内部接口 scope、tag 和 Authorization 安全要求")
        String internal() {
            return "internal";
        }
    }

    @RestController
    @RequestMapping("/doc-alias")
    @io.swagger.v3.oas.annotations.tags.Tag(name = "文档别名", description = "验证同一模块的多路径分组")
    static class AliasFlowController {

        @GetMapping("/ping")
        @PublicAccess(desc = "文档别名健康检查")
        @io.swagger.v3.oas.annotations.Operation(
                summary = "检查文档别名",
                description = "确认配置的第二个模块路径也进入同一 OpenAPI 分组")
        String ping() {
            return "pong";
        }
    }
}
