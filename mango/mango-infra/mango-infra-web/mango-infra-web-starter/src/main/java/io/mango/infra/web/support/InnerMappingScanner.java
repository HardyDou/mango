package io.mango.infra.web.support;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.infra.web.api.Inner;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
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
 * 发现标记了 @Inner 的 Spring MVC Handler 路径。
 */
public class InnerMappingScanner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final InnerMappingInternalPathProvider provider;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring MVC singleton collaborators are injected")
    public InnerMappingScanner(RequestMappingHandlerMapping handlerMapping,
                               InnerMappingInternalPathProvider provider) {
        this.handlerMapping = handlerMapping;
        this.provider = provider;
    }

    @EventListener(ApplicationStartedEvent.class)
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
                // 并非每个 Controller 方法都必须声明在接口上。
            }
        }
        return false;
    }

    private Set<String> extractPaths(RequestMappingInfo mappingInfo) {
        Set<String> paths = new LinkedHashSet<>();
        var pathPatternsCondition = mappingInfo.getPathPatternsCondition();
        if (pathPatternsCondition != null) {
            pathPatternsCondition.getPatternValues().forEach(paths::add);
        }
        var patternsCondition = mappingInfo.getPatternsCondition();
        if (patternsCondition != null) {
            paths.addAll(patternsCondition.getPatterns());
        }
        return paths;
    }
}
