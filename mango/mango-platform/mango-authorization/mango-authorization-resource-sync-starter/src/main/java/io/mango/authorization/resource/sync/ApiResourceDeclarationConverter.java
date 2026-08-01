package io.mango.authorization.resource.sync;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.resource.api.enums.ResourceExecutionPhase;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.builder.ResourceDeclarationBuilder;
import io.mango.resource.support.model.ResourceDeclaration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Converts authorization API resource definitions into Resource Registry declarations.
 */
public class ApiResourceDeclarationConverter {

    private static final String TARGET_MODULE = "authorization";

    public ResourceDeclaration toDeclaration(ApiResourceRegisterCommand command, String sourceModuleCode) {
        ResourceDeclaration declaration = ResourceDeclarationBuilder.create(ResourceTypes.API_RESOURCE)
                .id(stableResourceId(command))
                .version(1)
                .module(sourceModuleCode, command.getModuleName())
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
        declaration.setExecutionPhase(ResourceExecutionPhase.RUNTIME_EVENTUAL);
        return declaration;
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
