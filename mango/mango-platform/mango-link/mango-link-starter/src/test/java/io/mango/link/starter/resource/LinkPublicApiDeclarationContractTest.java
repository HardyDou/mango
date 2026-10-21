package io.mango.link.starter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.link.starter.controller.LinkOpenController;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 匿名公开接口契约：`/link/open/**` 的公开入口必须由 `API_RESOURCE` 声明为 PUBLIC，
 * 函数式跳转路由由 {@link LinkPublicRedirectResourceProvider} 声明，注解式控制器由声明文件覆盖。
 */
class LinkPublicApiDeclarationContractTest {

    private static final Path PUBLIC_API_DECLARATION =
            Path.of("src/main/resources/META-INF/mango/resources/link-common-public-api.yml");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    @DisplayName("网址公开控制器入口有精确的 PUBLIC 声明")
    void linkOpenControllerEndpointsHaveExactDeclarations() throws IOException {
        Map<String, JsonNode> declarations = declarationsByResourceCode();
        String rootPath = AnnotatedElementUtils.findMergedAnnotation(
                LinkOpenController.class, RequestMapping.class).value()[0];

        int publicMethods = 0;
        for (Method method : LinkOpenController.class.getDeclaredMethods()) {
            ApiAccess access = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccess.class);
            if (access == null || access.mode() != ApiResourceAccessMode.PUBLIC) {
                continue;
            }
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            assertThat(mapping).as(method.getName()).isNotNull();
            String resourceCode = mapping.method()[0].name() + ":" + rootPath + mapping.value()[0];
            JsonNode declaration = declarations.get(resourceCode);
            assertThat(declaration).as(resourceCode).isNotNull();
            assertThat(field(declaration, "httpMethod"))
                    .isEqualTo(resourceCode.substring(0, resourceCode.indexOf(':')));
            assertThat(field(declaration, "pathPattern"))
                    .isEqualTo(resourceCode.substring(resourceCode.indexOf(':') + 1));
            assertThat(field(declaration, "accessMode")).isEqualTo("PUBLIC");
            assertThat(field(declaration, "handlerClass")).isEqualTo(LinkOpenController.class.getName());
            assertThat(field(declaration, "handlerMethod")).isEqualTo(method.getName());
            assertThat(declaration.path("target-module").asText()).isEqualTo("authorization");
            publicMethods++;
        }

        assertThat(publicMethods).isEqualTo(1);
    }

    @Test
    @DisplayName("网址公开声明使用稳定身份且不声明受登录保护的路由")
    void linkPublicDeclarationsUseStableIdentity() throws IOException {
        JsonNode resource = resource();

        assertThat(resource.path("module-code").asText()).isEqualTo("link");
        assertThat(resource.path("module-name").asText()).isEqualTo("mango-link");
        Map<String, JsonNode> declarations = declarationsByResourceCode();
        declarations.forEach((resourceCode, declaration) -> {
            assertThat(resourceCode).startsWith("GET:/link/open/");
            assertThat(field(declaration, "moduleName")).isEqualTo("mango-link");
            assertThat(field(declaration, "accessMode")).isEqualTo("PUBLIC");
            assertThat(declaration.path("id").asText()).matches("\\d+");
            assertThat(declaration.path("biz-key").asText()).startsWith("api.mango-link.");
        });
    }

    @Test
    @DisplayName("函数式跳转路由的 PUBLIC 与 LOGIN 声明保持分离")
    void linkFunctionalRoutesKeepAccessModeSplit() {
        Map<String, String> accessModes = new LinkedHashMap<>();
        new LinkPublicRedirectResourceProvider().provide()
                .forEach(declaration -> accessModes.put(
                        value(declaration, "pathPattern"), value(declaration, "accessMode")));

        assertThat(accessModes)
                .containsEntry(LinkPublicRedirectResourceProvider.REDIRECT_PATH, "PUBLIC")
                .containsEntry(LinkPublicRedirectResourceProvider.JUMP_PATH, "PUBLIC")
                .containsEntry(LinkPublicRedirectResourceProvider.VISIBLE_REDIRECT_PATH, "LOGIN")
                .containsEntry(LinkPublicRedirectResourceProvider.VISIBLE_JUMP_PATH, "LOGIN");
    }

    private String value(ResourceDeclaration declaration, String name) {
        ResourceField field = declaration.getFields().get(name);
        assertThat(field).isNotNull();
        return String.valueOf(field.getValue());
    }

    private String field(JsonNode declaration, String name) {
        JsonNode value = declaration.path("fields").path(name).path("value");
        return value.isMissingNode() || value.isNull() ? null : value.asText();
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
