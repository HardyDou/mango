package io.mango.infra.kv.core.aspect;

import io.mango.infra.kv.api.*;
import io.mango.infra.kv.api.annotation.Cacheable;
import io.mango.infra.kv.api.annotation.Idempotent;
import io.mango.infra.kv.api.annotation.Locker;
import io.mango.infra.kv.api.annotation.RateLimit;
import io.mango.infra.kv.api.expression.KvContextContributor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AOP aspect for KV annotations.
 * Handles @Cacheable, @RateLimit, @Idempotent, @Locker.
 * Runtime variables can be extended by KvContextContributor.
 */
@Aspect
@Component
public class KvCapabilityAspect {

    private static final char SPEL_VARIABLE_PREFIX = '#';
    private static final char SPEL_BEAN_PREFIX = '@';
    private static final String TEMPLATE_EXPRESSION_MARKER = "#{";
    private static final String NULL_LITERAL = "null";
    private static final Logger LOGGER = LoggerFactory.getLogger(KvCapabilityAspect.class);
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final TemplateParserContext TEMPLATE_CONTEXT = new TemplateParserContext();

    /**
     * Cache compiled SPEL expressions.
     * SpelExpression is immutable and thread-safe after parsing.
     * Avoids re-parsing the same key expression on every method call.
     */
    private static final Map<String, org.springframework.expression.Expression> EXPRESSION_CACHE =
            new ConcurrentHashMap<>(32);

    private final ObjectProvider<ICache> cacheProvider;
    private final ObjectProvider<ILocker> lockerProvider;
    private final ObjectProvider<IRateLimiter> rateLimiterProvider;
    private final ObjectProvider<IIdempotent> idempotentProvider;
    private final ObjectProvider<ISerializer> serializerProvider;
    private final BeanFactory beanFactory;
    private final List<KvContextContributor> kvContextContributors;

    public KvCapabilityAspect(ObjectProvider<ICache> cacheProvider,
                              ObjectProvider<ILocker> lockerProvider,
                              ObjectProvider<IRateLimiter> rateLimiterProvider,
                              ObjectProvider<IIdempotent> idempotentProvider,
                              ObjectProvider<ISerializer> serializerProvider,
                              BeanFactory beanFactory,
                              List<KvContextContributor> kvContextContributors) {
        this.cacheProvider = cacheProvider;
        this.lockerProvider = lockerProvider;
        this.rateLimiterProvider = rateLimiterProvider;
        this.idempotentProvider = idempotentProvider;
        this.serializerProvider = serializerProvider;
        this.beanFactory = beanFactory;
        this.kvContextContributors = kvContextContributors;
    }

    // ==================== @Cacheable ====================

    @Around("@annotation(io.mango.infra.kv.api.annotation.Cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Cacheable annotation = method.getAnnotation(Cacheable.class);
        String key = resolveKey(annotation.key(), pjp);
        ICache cache = requireCapability(cacheProvider, ICache.class, "@Cacheable");
        ISerializer serializer = requireCapability(serializerProvider, ISerializer.class, "@Cacheable");

        // Try to get from cache
        String cached = cache.get(key);
        if (cached != null) {
            LOGGER.debug("Cache hit for key: {}", key);
            return deserializeResult(cached, method);
        }

        // Execute method
        Object result = pjp.proceed();

        // Store in cache if cacheValue is true
        if (annotation.cacheValue() && result != null) {
            cache.set(key, serializer.serialize(result), annotation.ttl());
            LOGGER.debug("Cache set for key: {}, ttl: {}s", key, annotation.ttl());
        }

        return result;
    }

    // ==================== @RateLimit ====================

    @Around("@annotation(io.mango.infra.kv.api.annotation.RateLimit)")
    public Object aroundRateLimit(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        String key = resolveKey(annotation.key(), pjp);
        IRateLimiter rateLimiter = requireCapability(rateLimiterProvider, IRateLimiter.class, "@RateLimit");

        if (!rateLimiter.tryAcquire(key, annotation.permits())) {
            throw new RateLimitExceededException("Rate limit exceeded for key: " + key);
        }

        return pjp.proceed();
    }

    // ==================== @Idempotent ====================

    @Around("@annotation(io.mango.infra.kv.api.annotation.Idempotent)")
    public Object aroundIdempotent(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Idempotent annotation = method.getAnnotation(Idempotent.class);
        String key = resolveKey(annotation.key(), pjp);
        IIdempotent idempotent = requireCapability(idempotentProvider, IIdempotent.class, "@Idempotent");

        if (idempotent.checkAndMark(key, annotation.window())) {
            throw new DuplicateOperationException("Duplicate operation detected for key: " + key);
        }

        return pjp.proceed();
    }

    // ==================== @Locker ====================

    @Around("@annotation(io.mango.infra.kv.api.annotation.Locker)")
    public Object aroundLocker(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Locker annotation = method.getAnnotation(Locker.class);
        String key = resolveKey(annotation.key(), pjp);
        ILocker locker = requireCapability(lockerProvider, ILocker.class, "@Locker");

        if (!locker.tryLock(key, annotation.ttl())) {
            throw new LockAcquisitionException("Failed to acquire lock for key: " + key);
        }

        try {
            return pjp.proceed();
        } finally {
            locker.unlock(key);
        }
    }

    // ==================== Helper methods ====================

    private Method getMethod(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        return signature.getMethod();
    }

    /**
     * Resolve cache key from SpEL expression.
     * Supports three forms:
     * <ul>
     *   <li>Static string: no '#' or '@' - returned as-is, no context needed</li>
     *   <li>SpEL template: contains '#{' - parsed with TemplateParserContext, supports #{#paramName}</li>
     *   <li>Direct SpEL expression: starts with '#' or '@'</li>
     * </ul>
     * Compiled expressions are cached to avoid re-parsing on every call.
     */
    private String resolveKey(String keyExpression, ProceedingJoinPoint pjp) {
        // Fast path: static key, no parsing needed
        if (keyExpression.indexOf(SPEL_VARIABLE_PREFIX) < 0 && keyExpression.indexOf(SPEL_BEAN_PREFIX) < 0) {
            return keyExpression;
        }

        // Build evaluation context with minimal setup
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("args", args);
        kvContextContributors.forEach(contributor -> contributor.contribute(context::setVariable));

        // SpEL template: #{#paramName} - cache compiled expression
        if (keyExpression.contains(TEMPLATE_EXPRESSION_MARKER)) {
            org.springframework.expression.Expression expr = EXPRESSION_CACHE.computeIfAbsent(
                    keyExpression,
                    k -> SPEL_PARSER.parseExpression(k, TEMPLATE_CONTEXT)
            );
            return String.valueOf(expr.getValue(context));
        }

        if (!isDirectExpression(keyExpression)) {
            throw new IllegalArgumentException("KV key expression must use SpEL template syntax, for example "
                    + "'user:#{#userId}', or be a direct SpEL expression starting with '#' or '@': "
                    + keyExpression);
        }

        // Direct SpEL expression: #paramName or @beanName.method()
        org.springframework.expression.Expression expr = EXPRESSION_CACHE.computeIfAbsent(
                keyExpression,
                k -> SPEL_PARSER.parseExpression(k)
        );
        Object value = expr.getValue(context);
        if (value == null) {
            return NULL_LITERAL;
        }
        return String.valueOf(value);
    }

    private boolean isDirectExpression(String keyExpression) {
        return keyExpression.startsWith(String.valueOf(SPEL_VARIABLE_PREFIX))
                || keyExpression.startsWith(String.valueOf(SPEL_BEAN_PREFIX));
    }

    private <T> T requireCapability(ObjectProvider<T> provider, Class<T> capabilityType, String annotationName) {
        T capability = provider.getIfAvailable();
        if (capability == null) {
            throw new IllegalStateException(annotationName + " requires " + capabilityType.getSimpleName()
                    + " but the capability bean is not enabled");
        }
        return capability;
    }

    private Object deserializeResult(String content, Method method) {
        if (content == null) {
            return null;
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        ISerializer serializer = requireCapability(serializerProvider, ISerializer.class, "@Cacheable");
        return serializer.deserialize(content, returnType);
    }

    // ==================== Exceptions ====================

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    public static class DuplicateOperationException extends RuntimeException {
        public DuplicateOperationException(String message) {
            super(message);
        }
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
