package io.mango.cms.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.starter.controller.CmsSiteController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 匿名公开接口契约：`/cms/open/**` 的每个公开入口都必须在 API_RESOURCE 声明中登记为 PUBLIC，
 * 否则未匹配资源的请求按默认 LOGIN 策略返回 401。
 */
class CmsPublicApiDeclarationContractTest {

    private static final Path PUBLIC_API_DECLARATION =
            Path.of("src/main/resources/META-INF/mango/resources/cms-common-public-api.yml");
    private static final String PUBLIC_FILE_RESOURCE_CODE = "GET:/cms/open/files/public-preview";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    @DisplayName("CMS 站点公开接口的每个 PUBLIC 入口都有精确声明")
    void cmsPublicSiteEndpointsHaveExactDeclarations() throws IOException {
        Map<String, JsonNode> declarations = declarationsByResourceCode();

        Map<String, JsonNode> expected = new LinkedHashMap<>();
        String rootPath = AnnotatedElementUtils.findMergedAnnotation(
                CmsSiteController.class, RequestMapping.class).value()[0];
        for (Method method : CmsSiteController.class.getDeclaredMethods()) {
            if (!isPublic(method)) {
                continue;
            }
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            String resourceCode = mapping.method()[0].name() + ":" + rootPath + mapping.value()[0];
            JsonNode declaration = declarations.get(resourceCode);
            assertThat(declaration).as(resourceCode).isNotNull();
            assertPublicDeclaration(declaration, resourceCode, CmsSiteController.class.getName(), method.getName());
            expected.put(resourceCode, declaration);
        }

        assertThat(expected).hasSize(8);
        List<String> declaredCodes = List.of(
                Stream.concat(expected.keySet().stream(), Stream.of(PUBLIC_FILE_RESOURCE_CODE))
                        .toArray(String[]::new));
        assertThat(declarations.keySet()).containsExactlyInAnyOrderElementsOf(declaredCodes);
    }

    @Test
    @DisplayName("CMS 公开文件入口同样声明为 PUBLIC")
    void cmsPublicFileEndpointIsDeclaredPublic() throws IOException {
        JsonNode declaration = declarationsByResourceCode().get(PUBLIC_FILE_RESOURCE_CODE);

        assertThat(declaration).isNotNull();
        assertPublicDeclaration(
                declaration,
                PUBLIC_FILE_RESOURCE_CODE,
                "io.mango.cms.starter.endpoint.CmsPublicFileEndpoint",
                "handle");
    }

    @Test
    @DisplayName("CMS 公开 API 声明使用稳定身份且不声明后台管理接口")
    void cmsPublicApiDeclarationsUseStableIdentity() throws IOException {
        JsonNode resource = resource();
        Map<String, JsonNode> declarations = declarationsByResourceCode();

        assertThat(resource.path("module-code").asText()).isEqualTo("cms");
        assertThat(resource.path("module-name").asText()).isEqualTo("mango-cms");
        declarations.forEach((resourceCode, declaration) -> {
            assertThat(declaration.path("target-module").asText()).isEqualTo("authorization");
            assertThat(resourceCode).startsWith("GET:/cms/open/");
        });
        assertThat(declarations.values().stream()
                .map(declaration -> declaration.path("id").asText())
                .distinct()
                .count()).isEqualTo(declarations.size());
        assertThat(declarations.values().stream()
                .map(declaration -> declaration.path("biz-key").asText())
                .distinct()
                .count()).isEqualTo(declarations.size());
        declarations.values().forEach(declaration -> assertThat(declaration.path("id").asText())
                .matches("\\d+"));
    }

    private void assertPublicDeclaration(JsonNode declaration, String resourceCode,
                                         String handlerClass, String handlerMethod) {
        JsonNode fields = declaration.path("fields");
        assertThat(declaration.path("biz-key").asText()).startsWith("api.mango-cms.");
        assertThat(fields.path("moduleName").path("value").asText()).isEqualTo("mango-cms");
        assertThat(fields.path("httpMethod").path("value").asText()).isEqualTo(resourceCode.substring(0, resourceCode.indexOf(':')));
        assertThat(fields.path("pathPattern").path("value").asText()).isEqualTo(resourceCode.substring(resourceCode.indexOf(':') + 1));
        assertThat(fields.path("resourceCode").path("value").asText()).isEqualTo(resourceCode);
        assertThat(fields.path("accessMode").path("value").asText()).isEqualTo("PUBLIC");
        assertThat(fields.path("handlerClass").path("value").asText()).isEqualTo(handlerClass);
        assertThat(fields.path("handlerMethod").path("value").asText()).isEqualTo(handlerMethod);
        assertThat(fields.path("description").path("value").asText()).isNotBlank();
    }

    private boolean isPublic(Method method) {
        ApiAccess methodAccess = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccess.class);
        if (methodAccess != null) {
            return methodAccess.mode() == ApiResourceAccessMode.PUBLIC;
        }
        ApiAccess classAccess = AnnotatedElementUtils.findMergedAnnotation(
                CmsSiteController.class, ApiAccess.class);
        return classAccess != null && classAccess.mode() == ApiResourceAccessMode.PUBLIC;
    }

    private JsonNode resource() throws IOException {
        assertThat(PUBLIC_API_DECLARATION).isRegularFile();
        return YAML.readTree(PUBLIC_API_DECLARATION.toFile()).path("mango").path("resource");
    }

    private Map<String, JsonNode> declarationsByResourceCode() throws IOException {
        Map<String, JsonNode> declarations = new LinkedHashMap<>();
        resource().path("declarations").path("API_RESOURCE").forEach(declaration -> {
            String resourceCode = declaration.path("fields").path("resourceCode").path("value").asText();
            JsonNode previous = declarations.put(resourceCode, declaration);
            assertThat(previous).as(resourceCode).isNull();
        });
        return declarations;
    }
}
