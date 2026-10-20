package io.mango.information.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.information.starter.controller.InformationPublicController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 匿名公开接口契约：`/information/open/**` 的每个公开入口都必须在 API_RESOURCE 声明中登记为 PUBLIC。
 * 未匹配资源的请求按默认 LOGIN 策略处理，匿名访问会返回 401。
 */
class InformationPublicApiDeclarationContractTest {

    private static final Path PUBLIC_API_DECLARATION =
            Path.of("src/main/resources/META-INF/mango/resources/information-common-public-api.yml");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<FunctionalRoute> FUNCTIONAL_PUBLIC_ROUTES = List.of(
            new FunctionalRoute("POST", "/information/open/forms/E2E_FORM/upload-ticket"),
            new FunctionalRoute("POST", "/information/open/forms/E2E_FORM/files"),
            new FunctionalRoute("GET", "/information/open/forms/E2E_FORM/files/detail"),
            new FunctionalRoute("GET", "/information/open/forms/E2E_FORM/files/preview"));
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    @DisplayName("信息采集控制器公开入口都有精确的 PUBLIC 声明")
    void informationPublicControllerEndpointsHaveExactDeclarations() throws IOException {
        Map<String, JsonNode> declarations = declarationsByResourceCode();
        String rootPath = AnnotatedElementUtils.findMergedAnnotation(
                InformationPublicController.class, RequestMapping.class).value()[0];

        int publicMethods = 0;
        for (Method method : InformationPublicController.class.getDeclaredMethods()) {
            ApiAccess access = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccess.class);
            if (access == null || access.mode() != ApiResourceAccessMode.PUBLIC) {
                continue;
            }
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            assertThat(mapping).as(method.getName()).isNotNull();
            String resourceCode = mapping.method()[0].name() + ":" + rootPath + mapping.value()[0];
            JsonNode declaration = declarations.get(resourceCode);
            assertThat(declaration).as(resourceCode).isNotNull();
            assertPublicDeclaration(
                    declaration, resourceCode, InformationPublicController.class.getName(), method.getName());
            publicMethods++;
        }

        assertThat(publicMethods).isEqualTo(2);
    }

    @Test
    @DisplayName("信息采集公开文件函数式路由被 PUBLIC 声明覆盖")
    void informationFunctionalPublicRoutesAreCovered() throws IOException {
        Map<String, JsonNode> declarations = declarationsByResourceCode();

        FUNCTIONAL_PUBLIC_ROUTES.forEach(route -> {
            boolean covered = declarations.values().stream().anyMatch(declaration ->
                    "PUBLIC".equals(field(declaration, "accessMode"))
                            && route.httpMethod().equals(field(declaration, "httpMethod"))
                            && PATH_MATCHER.match(field(declaration, "pathPattern"), route.path()));
            assertThat(covered).as(route.toString()).isTrue();
        });
    }

    @Test
    @DisplayName("信息采集公开声明使用稳定身份且全部为 PUBLIC")
    void informationPublicDeclarationsUseStableIdentity() throws IOException {
        JsonNode resource = resource();

        assertThat(resource.path("module-code").asText()).isEqualTo("information");
        assertThat(resource.path("module-name").asText()).isEqualTo("mango-information");
        Map<String, JsonNode> declarations = declarationsByResourceCode();
        declarations.forEach((resourceCode, declaration) -> {
            assertThat(resourceCode).contains(":/information/open/");
            assertThat(field(declaration, "moduleName")).isEqualTo("mango-information");
            assertThat(field(declaration, "accessMode")).isEqualTo("PUBLIC");
            assertThat(declaration.path("target-module").asText()).isEqualTo("authorization");
            assertThat(declaration.path("id").asText()).matches("\\d+");
            assertThat(declaration.path("biz-key").asText()).startsWith("api.information.");
        });
        assertThat(declarations.values().stream().map(d -> d.path("id").asText()).distinct().count())
                .isEqualTo(declarations.size());
        assertThat(declarations.values().stream().map(d -> d.path("biz-key").asText()).distinct().count())
                .isEqualTo(declarations.size());
    }

    private void assertPublicDeclaration(JsonNode declaration, String resourceCode,
                                         String handlerClass, String handlerMethod) {
        assertThat(field(declaration, "httpMethod"))
                .isEqualTo(resourceCode.substring(0, resourceCode.indexOf(':')));
        assertThat(field(declaration, "pathPattern"))
                .isEqualTo(resourceCode.substring(resourceCode.indexOf(':') + 1));
        assertThat(field(declaration, "resourceCode")).isEqualTo(resourceCode);
        assertThat(field(declaration, "accessMode")).isEqualTo("PUBLIC");
        assertThat(field(declaration, "handlerClass")).isEqualTo(handlerClass);
        assertThat(field(declaration, "handlerMethod")).isEqualTo(handlerMethod);
        assertThat(field(declaration, "description")).isNotBlank();
    }

    private String field(JsonNode declaration, String name) {
        JsonNode value = declaration.path("fields").path(name).path("value");
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private record FunctionalRoute(String httpMethod, String path) {
    }

    private JsonNode resource() throws IOException {
        assertThat(PUBLIC_API_DECLARATION).isRegularFile();
        return YAML.readTree(PUBLIC_API_DECLARATION.toFile()).path("mango").path("resource");
    }

    private Map<String, JsonNode> declarationsByResourceCode() throws IOException {
        Map<String, JsonNode> declarations = new LinkedHashMap<>();
        resource().path("declarations").path("API_RESOURCE").forEach(declaration -> {
            String resourceCode = field(declaration, "resourceCode");
            JsonNode previous = declarations.put(resourceCode, declaration);
            assertThat(previous).as(resourceCode).isNull();
        });
        return declarations;
    }
}
