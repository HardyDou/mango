package io.mango.infra.doc.starter;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
/**
 * 为 OpenAPI 操作标记访问范围。
 */
public class MangoApiScopeOperationCustomizer implements OperationCustomizer {

    public static final String INTERNAL_SCOPE = "internal";
    public static final String EXTERNAL_SCOPE = "external";
    public static final String SCOPE_EXTENSION = "x-mango-api-scope";
    private static final String INTERNAL_TAG = "对内接口";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        boolean internal = isInternal(handlerMethod);
        String scope = internal ? INTERNAL_SCOPE : EXTERNAL_SCOPE;
        operation.addExtension(SCOPE_EXTENSION, scope);
        if (internal) {
            operation.addTagsItem(INTERNAL_TAG);
        }
        return operation;
    }

    private boolean isInternal(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return false;
        }
        Class<?> beanType = handlerMethod.getBeanType();
        Method method = handlerMethod.getMethod();
        ApiAccess apiAccess = findApiAccess(method);
        if (apiAccess == null) {
            apiAccess = findApiAccess(beanType);
        }
        if (apiAccess == null) {
            apiAccess = findApiAccessOnInterface(beanType, method);
        }
        return apiAccess != null && apiAccess.mode() == ApiResourceAccessMode.INTERNAL;
    }

    private ApiAccess findApiAccess(Class<?> type) {
        return type == null ? null : AnnotatedElementUtils.findMergedAnnotation(type, ApiAccess.class);
    }

    private ApiAccess findApiAccess(Method method) {
        return method == null ? null : AnnotatedElementUtils.findMergedAnnotation(method, ApiAccess.class);
    }

    private ApiAccess findApiAccessOnInterface(Class<?> handlerType, Method handlerMethod) {
        if (handlerType == null || handlerMethod == null) {
            return null;
        }
        for (Class<?> interfaceType : handlerType.getInterfaces()) {
            ApiAccess apiAccess = findApiAccess(interfaceType);
            if (apiAccess != null) {
                return apiAccess;
            }
            try {
                Method interfaceMethod = interfaceType.getMethod(
                        handlerMethod.getName(),
                        handlerMethod.getParameterTypes());
                apiAccess = findApiAccess(interfaceMethod);
                if (apiAccess != null) {
                    return apiAccess;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
