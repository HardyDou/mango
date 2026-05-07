package io.mango.authorization.resource.sync;

import io.mango.authorization.api.ApiResourceApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.common.result.R;
import io.mango.infra.module.api.ModuleInfo;
import io.mango.infra.module.api.ModuleInfoRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 扫描 Spring MVC 映射并注册 API 资源。
 */
@Slf4j
public class ApiResourceSyncRunner implements ApplicationRunner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApiResourceApi apiResourceApi;
    private final ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider;
    private final ApiResourceSyncProperties properties;

    @Value("${mango.authorization.resource-sync.module-name:}")
    private String moduleName;

    @Value("${mango.authorization.resource-sync.mode:write}")
    private String syncMode;

    @Value("${mango.authorization.resource-sync.include-packages:io.mango}")
    private String includePackages;

    @Value("${mango.authorization.resource-sync.exclude-paths:/error,/actuator/**}")
    private String excludePaths;

    @Value("${mango.authorization.resource-sync.default-access-mode:LOGIN}")
    private ApiResourceAccessMode defaultAccessMode;

    public ApiResourceSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            ApiResourceApi apiResourceApi) {
        this(handlerMapping, apiResourceApi, new EmptyModuleInfoRegistryProvider(), new ApiResourceSyncProperties());
    }

    public ApiResourceSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            ApiResourceApi apiResourceApi,
            ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider) {
        this(handlerMapping, apiResourceApi, moduleInfoRegistryProvider, new ApiResourceSyncProperties());
    }

    public ApiResourceSyncRunner(
            RequestMappingHandlerMapping handlerMapping,
            ApiResourceApi apiResourceApi,
            ObjectProvider<ModuleInfoRegistry> moduleInfoRegistryProvider,
            ApiResourceSyncProperties properties) {
        this.handlerMapping = handlerMapping;
        this.apiResourceApi = apiResourceApi;
        this.moduleInfoRegistryProvider = moduleInfoRegistryProvider;
        this.properties = properties == null ? new ApiResourceSyncProperties() : properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ApiResourceRegisterCommand> resources = scanResources();
        if (resources.isEmpty()) {
            log.info("API resource sync skipped: no resources discovered");
            return;
        }
        if ("read".equalsIgnoreCase(syncMode)) {
            log.info("API resource sync read-only: discovered {} resources", resources.size());
            return;
        }
        R<ApiResourceRegisterResultVO> response = apiResourceApi.registerApiResources(resources);
        ApiResourceRegisterResultVO result = response != null && response.isSuccess() && response.getData() != null
                ? response.getData()
                : ApiResourceRegisterResultVO.empty();
        log.info("API resource sync complete: scanned={}, created={}, updated={}",
                result.scanned(), result.created(), result.updated());
    }

    private List<ApiResourceRegisterCommand> scanResources() {
        List<ApiResourceRegisterCommand> resources = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handler = entry.getValue();
            if (!shouldInclude(handler)) {
                continue;
            }
            Set<String> paths = resolvePaths(entry.getKey());
            Set<String> methods = resolveMethods(entry.getKey());
            for (String path : paths) {
                if (isExcludedPath(path)) {
                    continue;
                }
                for (String httpMethod : methods) {
                    resources.add(toDefinition(handler, httpMethod, path));
                }
            }
        }
        resources.addAll(configuredResources());
        resources.sort(Comparator
                .comparing(ApiResourceRegisterCommand::getPathPattern)
                .thenComparing(ApiResourceRegisterCommand::getHttpMethod));
        return resources;
    }

    private List<ApiResourceRegisterCommand> configuredResources() {
        if (properties.getResources() == null || properties.getResources().isEmpty()) {
            return List.of();
        }
        return properties.getResources().stream()
                .filter(resource -> StringUtils.hasText(resource.getPathPattern()))
                .map(this::toConfiguredDefinition)
                .toList();
    }

    private ApiResourceRegisterCommand toConfiguredDefinition(ApiResourceSyncProperties.Resource resource) {
        String httpMethod = StringUtils.hasText(resource.getHttpMethod())
                ? resource.getHttpMethod().trim().toUpperCase()
                : "ALL";
        ApiResourceAccessMode accessMode = resource.getAccessMode() == null
                ? defaultAccessMode
                : resource.getAccessMode();
        if (accessMode == ApiResourceAccessMode.PERMISSION && !StringUtils.hasText(resource.getPermissionCode())) {
            throw new IllegalStateException("Configured API resource PERMISSION requires permission: "
                    + httpMethod + " " + resource.getPathPattern());
        }

        ApiResourceRegisterCommand definition = new ApiResourceRegisterCommand();
        definition.setModuleName(StringUtils.hasText(resource.getModuleName())
                ? resource.getModuleName()
                : fallbackModuleName());
        definition.setHttpMethod(httpMethod);
        definition.setPathPattern(resource.getPathPattern());
        definition.setAccessMode(accessMode);
        definition.setPermissionCode(resource.getPermissionCode());
        definition.setResourceCode(StringUtils.hasText(resource.getResourceCode())
                ? resource.getResourceCode()
                : (StringUtils.hasText(resource.getPermissionCode())
                        ? resource.getPermissionCode()
                        : httpMethod + ":" + resource.getPathPattern()));
        definition.setHandlerClass("configuration");
        definition.setHandlerMethod("mango.authorization.resource-sync.resources");
        definition.setDescription(StringUtils.hasText(resource.getDescription())
                ? resource.getDescription()
                : "Configured API resource");
        return definition;
    }

    private boolean shouldInclude(HandlerMethod handler) {
        Class<?> beanType = handler.getBeanType();
        String className = beanType.getName();
        return splitConfig(includePackages).stream().anyMatch(className::startsWith);
    }

    private Set<String> resolvePaths(RequestMappingInfo info) {
        Set<String> paths = new LinkedHashSet<>();
        if (info.getPathPatternsCondition() != null) {
            paths.addAll(info.getPathPatternsCondition().getPatternValues());
        }
        PatternsRequestCondition patternsCondition = info.getPatternsCondition();
        if (patternsCondition != null) {
            paths.addAll(patternsCondition.getPatterns());
        }
        return paths;
    }

    private Set<String> resolveMethods(RequestMappingInfo info) {
        RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();
        Set<RequestMethod> requestMethods = methodsCondition.getMethods();
        if (requestMethods.isEmpty()) {
            return Set.of("ALL");
        }
        Set<String> methods = new LinkedHashSet<>();
        requestMethods.forEach(method -> methods.add(method.name()));
        return methods;
    }

    private ApiResourceRegisterCommand toDefinition(HandlerMethod handler, String httpMethod, String path) {
        Method method = handler.getMethod();
        AccessDeclaration access = resolveAccess(handler.getBeanType(), method);
        ApiResourceRegisterCommand definition = new ApiResourceRegisterCommand();
        definition.setModuleName(resolveModuleName(path));
        definition.setHttpMethod(httpMethod);
        definition.setPathPattern(path);
        definition.setHandlerClass(handler.getBeanType().getName());
        definition.setHandlerMethod(method.getName());
        definition.setDescription(StringUtils.hasText(access.description())
                ? access.description()
                : handler.getBeanType().getSimpleName() + "#" + method.getName());
        if (access.mode() == ApiResourceAccessMode.PERMISSION) {
            if (!StringUtils.hasText(access.permissionCode())) {
                throw new IllegalStateException("@ApiAccess PERMISSION requires permission: "
                        + handler.getBeanType().getName() + "#" + method.getName());
            }
            definition.setPermissionCode(access.permissionCode());
            definition.setResourceCode(access.permissionCode());
            definition.setAccessMode(ApiResourceAccessMode.PERMISSION);
        } else {
            definition.setResourceCode(httpMethod + ":" + path);
            definition.setAccessMode(access.mode());
        }
        return definition;
    }

    private String resolveModuleName(String path) {
        ModuleInfoRegistry registry = moduleInfoRegistryProvider.getIfAvailable();
        if (registry == null) {
            return fallbackModuleName();
        }
        return registry.resolveByRequestPath(path)
                .map(ModuleInfo::moduleName)
                .orElseGet(this::fallbackModuleName);
    }

    private String fallbackModuleName() {
        return StringUtils.hasText(moduleName) ? moduleName : "unknown-module";
    }

    private AccessDeclaration resolveAccess(Class<?> handlerType, Method handlerMethod) {
        ApiAccess apiAccess = findApiAccess(handlerType, handlerMethod);
        if (apiAccess != null) {
            return new AccessDeclaration(apiAccess.mode(), apiAccess.permission(), apiAccess.desc());
        }
        return new AccessDeclaration(defaultAccessMode, null, null);
    }

    private ApiAccess findApiAccess(Class<?> handlerType, Method handlerMethod) {
        Class<?> targetType = handlerType;
        Method targetMethod = targetMethod(targetType, handlerMethod);
        ApiAccess methodAccess = findMethodApiAccess(targetType, targetMethod);
        if (methodAccess != null) {
            return methodAccess;
        }
        return findTypeApiAccess(targetType);
    }

    private ApiAccess findMethodApiAccess(Class<?> handlerType, Method handlerMethod) {
        ApiAccess apiAccess = AnnotatedElementUtils.findMergedAnnotation(handlerMethod, ApiAccess.class);
        if (apiAccess != null) {
            return apiAccess;
        }
        for (Class<?> interfaceType : handlerType.getInterfaces()) {
            try {
                Method interfaceMethod = interfaceType.getMethod(handlerMethod.getName(), handlerMethod.getParameterTypes());
                apiAccess = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, ApiAccess.class);
                if (apiAccess != null) {
                    return apiAccess;
                }
            } catch (NoSuchMethodException ignored) {
                // 控制器方法不一定声明在 API 接口上。
            }
        }
        return null;
    }

    private ApiAccess findTypeApiAccess(Class<?> handlerType) {
        ApiAccess apiAccess = AnnotatedElementUtils.findMergedAnnotation(handlerType, ApiAccess.class);
        if (apiAccess != null) {
            return apiAccess;
        }
        for (Class<?> interfaceType : handlerType.getInterfaces()) {
            apiAccess = AnnotatedElementUtils.findMergedAnnotation(interfaceType, ApiAccess.class);
            if (apiAccess != null) {
                return apiAccess;
            }
        }
        return null;
    }

    private Method targetMethod(Class<?> targetType, Method handlerMethod) {
        if (targetType == null || handlerMethod == null) {
            return handlerMethod;
        }
        return AopUtils.getMostSpecificMethod(handlerMethod, targetType);
    }

    private boolean isExcludedPath(String path) {
        return splitConfig(excludePaths).stream().anyMatch(pattern -> matchPath(pattern, path));
    }

    private boolean matchPath(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return false;
    }

    private List<String> splitConfig(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private record AccessDeclaration(
            ApiResourceAccessMode mode,
            String permissionCode,
            String description) {
    }

    private static class EmptyModuleInfoRegistryProvider implements ObjectProvider<ModuleInfoRegistry> {

        @Override
        public ModuleInfoRegistry getObject(Object... args) {
            return null;
        }

        @Override
        public ModuleInfoRegistry getIfAvailable() {
            return null;
        }

        @Override
        public ModuleInfoRegistry getIfUnique() {
            return null;
        }

        @Override
        public ModuleInfoRegistry getObject() {
            return null;
        }
    }
}
