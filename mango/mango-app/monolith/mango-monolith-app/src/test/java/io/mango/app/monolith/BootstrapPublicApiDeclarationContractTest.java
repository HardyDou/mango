package io.mango.app.monolith;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.starter.controller.AuthController;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.resource.api.enums.ResourceExecutionPhase;
import io.mango.resource.support.config.ResourceRegistryProperties;
import io.mango.resource.support.declaration.ResourceDeclarationLoader;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.system.starter.controller.AdminBrandingController;
import io.mango.system.starter.controller.SysTenantController;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapPublicApiDeclarationContractTest {

    private static final List<Class<?>> LOGIN_ENTRY_CONTROLLERS = List.of(
            AuthController.class,
            AdminBrandingController.class,
            SysTenantController.class);

    @Test
    void everyLoginEntryPublicApiHasAnExactBootstrapDeclaration() {
        Map<ApiKey, PublicEndpoint> publicEndpoints = LOGIN_ENTRY_CONTROLLERS.stream()
                .flatMap(controller -> publicEndpoints(controller).stream())
                .collect(Collectors.toMap(PublicEndpoint::key, Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate public endpoint " + left.key());
                        }, LinkedHashMap::new));
        Map<ApiKey, ResourceDeclaration> declarations = bootstrapApiDeclarations().stream()
                .filter(declaration -> publicEndpoints.containsKey(key(declaration)))
                .collect(Collectors.toMap(this::key, Function.identity()));

        assertThat(declarations).containsOnlyKeys(publicEndpoints.keySet());
        publicEndpoints.forEach((key, endpoint) -> assertExactDeclaration(endpoint, declarations.get(key)));
    }

    @Test
    void adjacentProtectedApisRemainOutsideTheBootstrapPublicContract() throws Exception {
        assertProtected(AuthController.class, "logout");
        assertProtected(AuthController.class, "info");
        assertProtected(AdminBrandingController.class, "get");
        assertProtected(SysTenantController.class, "list");

        Set<ApiKey> declared = bootstrapApiDeclarations().stream()
                .map(this::key)
                .collect(Collectors.toSet());
        assertThat(declared)
                .doesNotContain(
                        new ApiKey("POST", "/auth/logout"),
                        new ApiKey("GET", "/auth/info"),
                        new ApiKey("GET", "/system/admin-branding"),
                        new ApiKey("GET", "/system/tenant/list"));
    }

    private List<PublicEndpoint> publicEndpoints(Class<?> controller) {
        String rootPath = mappingPath(AnnotatedElementUtils.findMergedAnnotation(
                controller, RequestMapping.class));
        return Arrays.stream(controller.getDeclaredMethods())
                .map(method -> publicEndpoint(controller, rootPath, method))
                .filter(endpoint -> endpoint != null)
                .toList();
    }

    private PublicEndpoint publicEndpoint(Class<?> controller, String rootPath, Method method) {
        ApiAccess access = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccess.class);
        if (access == null || access.mode() != ApiResourceAccessMode.PUBLIC) {
            return null;
        }
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        assertThat(mapping).as(controller.getName() + "#" + method.getName()).isNotNull();
        assertThat(mapping.method()).hasSize(1);
        String path = normalizePath(rootPath + mappingPath(mapping));
        String moduleName = path.startsWith("/auth/") ? "mango-auth" : "mango-system";
        return new PublicEndpoint(
                new ApiKey(mapping.method()[0].name(), path),
                moduleName,
                controller.getName(),
                method.getName(),
                access.desc());
    }

    private void assertExactDeclaration(PublicEndpoint endpoint, ResourceDeclaration declaration) {
        assertThat(declaration).as(endpoint.key().toString()).isNotNull();
        assertThat(declaration.getId()).isEqualTo(stableResourceId(endpoint));
        assertThat(declaration.getVersion()).isEqualTo(1);
        assertThat(declaration.getResourceType()).isEqualTo("API_RESOURCE");
        assertThat(declaration.getModuleCode()).isEqualTo("authorization");
        assertThat(declaration.getModuleName()).isEqualTo(endpoint.moduleName());
        assertThat(declaration.getBizKey()).isEqualTo(bizKey(endpoint));
        assertThat(declaration.getName()).isEqualTo(endpoint.description());
        assertThat(declaration.getTargetModule()).isEqualTo("authorization");
        assertThat(declaration.getExecutionPhase()).isEqualTo(ResourceExecutionPhase.BOOTSTRAP_REQUIRED);
        assertThat(stringField(declaration, "moduleName")).isEqualTo(endpoint.moduleName());
        assertThat(stringField(declaration, "httpMethod")).isEqualTo(endpoint.key().httpMethod());
        assertThat(stringField(declaration, "pathPattern")).isEqualTo(endpoint.key().path());
        assertThat(stringField(declaration, "resourceCode"))
                .isEqualTo(endpoint.key().httpMethod() + ":" + endpoint.key().path());
        assertThat(stringField(declaration, "permissionCode")).isNull();
        assertThat(stringField(declaration, "accessMode")).isEqualTo("PUBLIC");
        assertThat(stringField(declaration, "handlerClass")).isEqualTo(endpoint.handlerClass());
        assertThat(stringField(declaration, "handlerMethod")).isEqualTo(endpoint.handlerMethod());
        assertThat(stringField(declaration, "description")).isEqualTo(endpoint.description());
    }

    private List<ResourceDeclaration> bootstrapApiDeclarations() {
        ResourceRegistryProperties properties = new ResourceRegistryProperties();
        return new ResourceDeclarationLoader(new ObjectMapper(), properties).load().stream()
                .filter(declaration -> "API_RESOURCE".equals(declaration.getResourceType()))
                .filter(declaration -> declaration.getExecutionPhase() == ResourceExecutionPhase.BOOTSTRAP_REQUIRED)
                .filter(declaration -> LOGIN_ENTRY_CONTROLLERS.stream()
                        .map(Class::getName)
                        .anyMatch(name -> name.equals(stringField(declaration, "handlerClass"))))
                .toList();
    }

    private void assertProtected(Class<?> controller, String methodName) throws Exception {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow(NoSuchMethodException::new);
        ApiAccess access = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccess.class);
        assertThat(access).isNotNull();
        assertThat(access.mode()).isNotEqualTo(ApiResourceAccessMode.PUBLIC);
    }

    private ApiKey key(ResourceDeclaration declaration) {
        return new ApiKey(stringField(declaration, "httpMethod"), stringField(declaration, "pathPattern"));
    }

    private String stringField(ResourceDeclaration declaration, String fieldName) {
        ResourceField field = declaration.getFields().get(fieldName);
        return field == null || field.getValue() == null ? null : String.valueOf(field.getValue());
    }

    private String mappingPath(RequestMapping mapping) {
        assertThat(mapping).isNotNull();
        String[] paths = mapping.path().length == 0 ? mapping.value() : mapping.path();
        return paths.length == 0 ? "" : paths[0];
    }

    private String normalizePath(String path) {
        return path.replaceAll("/{2,}", "/");
    }

    private String stableResourceId(PublicEndpoint endpoint) {
        byte[] digest = sha256("API_RESOURCE\n" + endpoint.moduleName() + "\n"
                + endpoint.key().httpMethod() + "\n" + endpoint.key().path());
        long value = 0L;
        for (int index = 0; index < Long.BYTES; index++) {
            value = (value << 8) | (digest[index] & 0xffL);
        }
        return String.valueOf(800000000000000000L + Math.floorMod(value, 100000000000000000L));
    }

    private String bizKey(PublicEndpoint endpoint) {
        String pathKey = endpoint.key().path()
                .replace('/', '.')
                .replace(':', '.')
                .replace('*', 'x')
                .replace('{', '.')
                .replace('}', '.')
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");
        return "api." + endpoint.moduleName() + "." + endpoint.key().httpMethod() + "." + pathKey;
    }

    private byte[] sha256(String source) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private record ApiKey(String httpMethod, String path) {
    }

    private record PublicEndpoint(
            ApiKey key,
            String moduleName,
            String handlerClass,
            String handlerMethod,
            String description) {
    }
}
