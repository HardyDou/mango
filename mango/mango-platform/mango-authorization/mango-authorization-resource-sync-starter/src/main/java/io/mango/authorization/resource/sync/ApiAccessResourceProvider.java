package io.mango.authorization.resource.sync;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.builder.ResourceDeclarationBuilder;
import io.mango.resource.api.model.ResourceDeclaration;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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
        return ResourceDeclarationBuilder.create(ResourceTypes.API_RESOURCE)
                .id(stableResourceId(command))
                .version(1)
                .module(properties.getProviderModuleCode(), command.getModuleName())
                .bizKey(bizKey(command))
                .name(command.getDescription())
                .targetModule(TARGET_MODULE)
                .string("moduleName", command.getModuleName())
                .string("httpMethod", command.getHttpMethod())
                .string("pathPattern", command.getPathPattern())
                .string("resourceCode", command.getResourceCode())
                .string("permissionCode", command.getPermissionCode())
                .string("accessMode", command.getAccessMode() == null ? null : command.getAccessMode().name())
                .string("handlerClass", command.getHandlerClass())
                .string("handlerMethod", command.getHandlerMethod())
                .string("description", command.getDescription())
                .build();
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
