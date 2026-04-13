package io.mango.infra.kv.core.aspect;

import io.mango.infra.kv.api.*;
import io.mango.infra.kv.api.annotation.Cacheable;
import io.mango.infra.kv.api.annotation.Idempotent;
import io.mango.infra.kv.api.annotation.Locker;
import io.mango.infra.kv.api.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP aspect for DAL annotations.
 * Handles @Cacheable, @RateLimit, @Idempotent, @Locker.
 */
@Aspect
@Component
public class DalAspect {

    private static final Logger log = LoggerFactory.getLogger(DalAspect.class);

    private final ICache cache;
    private final ILocker locker;
    private final ICounter counter;
    private final IRateLimiter rateLimiter;
    private final IIdempotent idempotent;
    private final ISerializer serializer;

    private static final java.util.regex.Pattern SPEL_PARAM_PATTERN = java.util.regex.Pattern.compile("#(\\w+)");

    public DalAspect(ICache cache, ILocker locker, ICounter counter,
                     IRateLimiter rateLimiter, IIdempotent idempotent, ISerializer serializer) {
        this.cache = cache;
        this.locker = locker;
        this.counter = counter;
        this.rateLimiter = rateLimiter;
        this.idempotent = idempotent;
        this.serializer = serializer;
    }

    // ==================== @Cacheable ====================

    @Around("@annotation(io.mango.infra.kv.api.annotation.Cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        Cacheable annotation = method.getAnnotation(Cacheable.class);
        String key = resolveKey(annotation.key(), pjp);

        // Try to get from cache
        String cached = cache.get(key);
        if (cached != null) {
            log.debug("Cache hit for key: {}", key);
            return deserializeResult(cached, method);
        }

        // Execute method
        Object result = pjp.proceed();

        // Store in cache if cacheValue is true
        if (annotation.cacheValue() && result != null) {
            cache.set(key, serializer.serialize(result), annotation.ttl());
            log.debug("Cache set for key: {}, ttl: {}s", key, annotation.ttl());
        }

        return result;
    }

    // ==================== @RateLimit ====================

    @Around("@annotation(io.mango.infra.kv.api.annotation.RateLimit)")
    public Object aroundRateLimit(ProceedingJoinPoint pjp) throws Throwable {
        Method method = getMethod(pjp);
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        String key = resolveKey(annotation.key(), pjp);

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

    private String resolveKey(String keyExpression, ProceedingJoinPoint pjp) {
        if (!keyExpression.contains("#")) {
            return keyExpression;
        }

        // Build evaluation context from method arguments
        StandardEvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        // Replace each #param reference with its value
        String result = keyExpression;
        java.util.regex.Matcher matcher = SPEL_PARAM_PATTERN.matcher(keyExpression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object paramValue = context.lookupVariable(paramName);
            matcher.appendReplacement(sb, paramValue != null ? paramValue.toString() : "null");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object deserializeResult(String content, Method method) {
        if (content == null) {
            return null;
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
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