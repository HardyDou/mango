package io.mango.authorization.resource.sync;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 Spring MVC API 访问声明提供给 mango-resource。
 */
@RequiredArgsConstructor
public class ApiAccessResourceProvider implements ResourceProvider {

    private static final String TARGET_MODULE = "authorization";

    private final ApiAccessResourceDiscoverer discoverer;
    private final ApiResourceSyncProperties properties;

    @Override
    public List<String> moduleCodes() {
        return List.of(properties.getProviderModuleCode());
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return discoverer.discover().stream()
                .map(this::toDeclaration)
                .toList();
    }

    private ResourceDeclaration toDeclaration(ApiResourceRegisterCommand command) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(stableResourceId(command));
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.API_RESOURCE);
        declaration.setModuleCode(properties.getProviderModuleCode());
        declaration.setModuleName(command.getModuleName());
        declaration.setBizKey(bizKey(command));
        declaration.setName(command.getDescription());
        declaration.setTargetModule(TARGET_MODULE);
        declaration.setFields(fields(command));
        return declaration;
    }

    private Map<String, ResourceField> fields(ApiResourceRegisterCommand command) {
        Map<String, ResourceField> fields = new LinkedHashMap<>();
        put(fields, "moduleName", command.getModuleName());
        put(fields, "httpMethod", command.getHttpMethod());
        put(fields, "pathPattern", command.getPathPattern());
        put(fields, "resourceCode", command.getResourceCode());
        put(fields, "permissionCode", command.getPermissionCode());
        put(fields, "accessMode", command.getAccessMode() == null ? null : command.getAccessMode().name());
        put(fields, "handlerClass", command.getHandlerClass());
        put(fields, "handlerMethod", command.getHandlerMethod());
        put(fields, "description", command.getDescription());
        return fields;
    }

    private void put(Map<String, ResourceField> fields, String name, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        ResourceField field = new ResourceField();
        field.setType(ResourceFieldType.STRING);
        field.setValue(value);
        fields.put(name, field);
    }

    private String bizKey(ApiResourceRegisterCommand command) {
        return "api." + command.getModuleName()
                + "." + command.getHttpMethod()
                + "." + command.getPathPattern()
                .replace('/', '.')
                .replace(':', '.')
                .replace('*', 'x')
                .replace('{', '.')
                .replace('}', '.')
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");
    }

    private String stableResourceId(ApiResourceRegisterCommand command) {
        String source = ResourceTypes.API_RESOURCE + "\n"
                + command.getModuleName() + "\n"
                + command.getHttpMethod() + "\n"
                + command.getPathPattern();
        byte[] digest = sha256(source);
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (digest[i] & 0xffL);
        }
        return String.valueOf(800000000000000000L + Math.floorMod(value, 100000000000000000L));
    }

    private byte[] sha256(String source) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
