package io.mango.infra.web.support;

import io.mango.infra.web.api.Inner;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Discovers Spring MVC handler paths marked with @Inner.
 */
public class InnerMappingScanner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final InnerMappingInternalPathProvider provider;

    public InnerMappingScanner(RequestMappingHandlerMapping handlerMapping,
                               InnerMappingInternalPathProvider provider) {
        this.handlerMapping = handlerMapping;
        this.provider = provider;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void scan() {
        Set<String> paths = new LinkedHashSet<>();
        for (Map.Entry<RequestMappingInfo, org.springframework.web.method.HandlerMethod> entry
                : handlerMapping.getHandlerMethods().entrySet()) {
            if (isInner(entry.getValue().getBeanType(), entry.getValue().getMethod())) {
                paths.addAll(extractPaths(entry.getKey()));
            }
        }
        provider.replacePaths(paths);
    }

    private boolean isInner(Class<?> handlerType, Method handlerMethod) {
        Class<?> targetType = AopUtils.getTargetClass(handlerType);
        return hasInner(targetType)
                || hasInner(handlerMethod)
                || hasInnerOnInterface(targetType, handlerMethod);
    }

    private boolean hasInner(Class<?> type) {
        return type != null && AnnotatedElementUtils.hasAnnotation(type, Inner.class);
    }

    private boolean hasInner(Method method) {
        return method != null && AnnotatedElementUtils.hasAnnotation(method, Inner.class);
    }

    private boolean hasInnerOnInterface(Class<?> handlerType, Method handlerMethod) {
        if (handlerType == null || handlerMethod == null) {
            return false;
        }
        for (Class<?> interfaceType : handlerType.getInterfaces()) {
            if (hasInner(interfaceType)) {
                return true;
            }
            try {
                Method interfaceMethod = interfaceType.getMethod(handlerMethod.getName(), handlerMethod.getParameterTypes());
                if (hasInner(interfaceMethod)) {
                    return true;
                }
            } catch (NoSuchMethodException ignored) {
                // Not every controller method must be declared on an interface.
            }
        }
        return false;
    }

    private Set<String> extractPaths(RequestMappingInfo mappingInfo) {
        Set<String> paths = new LinkedHashSet<>();
        if (mappingInfo.getPathPatternsCondition() != null) {
            mappingInfo.getPathPatternsCondition().getPatternValues().forEach(paths::add);
        }
        if (mappingInfo.getPatternsCondition() != null) {
            paths.addAll(mappingInfo.getPatternsCondition().getPatterns());
        }
        return paths;
    }
}
