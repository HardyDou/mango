package io.mango.infra.persistence.starter.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * {@link PersistenceDataSource} 注解路由。
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PersistenceDataSourceAspect {

    @Around("@within(io.mango.infra.persistence.starter.datasource.PersistenceDataSource) "
            + "|| @annotation(io.mango.infra.persistence.starter.datasource.PersistenceDataSource)")
    public Object route(ProceedingJoinPoint point) throws Throwable {
        String dataSourceName = resolveDataSourceName(point);
        if (!StringUtils.hasText(dataSourceName)) {
            return point.proceed();
        }
        try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use(dataSourceName)) {
            return point.proceed();
        }
    }

    private String resolveDataSourceName(ProceedingJoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        PersistenceDataSource methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method,
                PersistenceDataSource.class);
        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }
        Class<?> targetClass = point.getTarget() == null ? method.getDeclaringClass() : point.getTarget().getClass();
        PersistenceDataSource classAnnotation = AnnotatedElementUtils.findMergedAnnotation(targetClass,
                PersistenceDataSource.class);
        return classAnnotation == null ? "" : classAnnotation.value();
    }
}
