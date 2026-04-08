package io.mango.dal.core.aspect;

import io.mango.dal.api.ICache;
import io.mango.dal.api.ICounter;
import io.mango.dal.api.IIdempotent;
import io.mango.dal.api.ILocker;
import io.mango.dal.api.IRateLimiter;
import io.mango.dal.api.ISerializer;
import io.mango.dal.api.annotation.Cacheable;
import io.mango.dal.api.annotation.Idempotent;
import io.mango.dal.api.annotation.Locker;
import io.mango.dal.api.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DalAspect.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DalAspectTest {

    @Mock
    private ICache cache;

    @Mock
    private ILocker locker;

    @Mock
    private ICounter counter;

    @Mock
    private IRateLimiter rateLimiter;

    @Mock
    private IIdempotent idempotent;

    @Mock
    private ISerializer serializer;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature signature;

    private DalAspect dalAspect;

    @BeforeEach
    void setUp() {
        dalAspect = new DalAspect(cache, locker, counter, rateLimiter, idempotent, serializer);
    }

    // ==================== @RateLimit tests ====================

    @Test
    void aroundRateLimit_withinLimit_proceeds() throws Throwable {
        Method method = TestService.class.getMethod("rateLimitedMethod", String.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"1"});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"userId"});
        when(rateLimiter.tryAcquire("rate:user:1", 1)).thenReturn(true);
        when(pjp.proceed()).thenReturn("result");

        Object result = dalAspect.aroundRateLimit(pjp);

        assertEquals("result", result);
        verify(rateLimiter).tryAcquire("rate:user:1", 1);
    }

    @Test
    void aroundRateLimit_exceeded_throwsException() throws Throwable {
        Method method = TestService.class.getMethod("rateLimitedMethod", String.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"1"});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"userId"});
        when(rateLimiter.tryAcquire("rate:user:1", 1)).thenReturn(false);

        assertThrows(DalAspect.RateLimitExceededException.class, () -> dalAspect.aroundRateLimit(pjp));
        verify(pjp, never()).proceed();
    }

    // ==================== @Idempotent tests ====================

    @Test
    void aroundIdempotent_newOperation_proceeds() throws Throwable {
        Method method = TestService.class.getMethod("idempotentMethod", String.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"123"});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"orderNo"});
        when(idempotent.checkAndMark("idempot:order:123", 60)).thenReturn(false);
        when(pjp.proceed()).thenReturn("result");

        Object result = dalAspect.aroundIdempotent(pjp);

        assertEquals("result", result);
        verify(idempotent).checkAndMark("idempot:order:123", 60);
    }

    @Test
    void aroundIdempotent_duplicate_throwsException() throws Throwable {
        Method method = TestService.class.getMethod("idempotentMethod", String.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"123"});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"orderNo"});
        when(idempotent.checkAndMark("idempot:order:123", 60)).thenReturn(true);

        assertThrows(DalAspect.DuplicateOperationException.class, () -> dalAspect.aroundIdempotent(pjp));
        verify(pjp, never()).proceed();
    }

    @Test
    void aroundIdempotent_proceedThrows_exceptionPropagates() throws Throwable {
        Method method = TestService.class.getMethod("idempotentMethod", String.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"123"});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"orderNo"});
        when(idempotent.checkAndMark("idempot:order:123", 60)).thenReturn(false);
        when(pjp.proceed()).thenThrow(new RuntimeException("operation failed"));

        assertThrows(RuntimeException.class, () -> dalAspect.aroundIdempotent(pjp));
    }

    // ==================== @Locker tests ====================

    @Test
    void aroundLocker_lockAcquired_proceedsAndUnlocks() throws Throwable {
        Method method = TestService.class.getMethod("lockedMethod", Long.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{123L});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"orderId"});
        when(locker.tryLock("lock:order:123", 30)).thenReturn(true);
        when(pjp.proceed()).thenReturn("result");

        Object result = dalAspect.aroundLocker(pjp);

        assertEquals("result", result);
        verify(locker).tryLock("lock:order:123", 30);
        verify(locker).unlock("lock:order:123");
    }

    @Test
    void aroundLocker_lockFailed_throwsException() throws Throwable {
        Method method = TestService.class.getMethod("lockedMethod", Long.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{123L});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"orderId"});
        when(locker.tryLock("lock:order:123", 30)).thenReturn(false);

        assertThrows(DalAspect.LockAcquisitionException.class, () -> dalAspect.aroundLocker(pjp));
        verify(pjp, never()).proceed();
    }

    @Test
    void aroundLocker_exception_releasesLock() throws Throwable {
        Method method = TestService.class.getMethod("lockedMethod", Long.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{123L});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"orderId"});
        when(locker.tryLock("lock:order:123", 30)).thenReturn(true);
        when(pjp.proceed()).thenThrow(new RuntimeException("error"));

        assertThrows(RuntimeException.class, () -> dalAspect.aroundLocker(pjp));
        verify(locker).unlock("lock:order:123");
    }

    // ==================== @Cacheable tests ====================

    @Test
    void aroundCacheable_cacheHit_returnsCached() throws Throwable {
        Method method = TestService.class.getMethod("cachedMethod", String.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"1"});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"userId"});
        when(signature.getReturnType()).thenReturn((Class) String.class);
        when(cache.get("cache:user:1")).thenReturn("\"cachedValue\"");
        when(serializer.deserialize("\"cachedValue\"", String.class)).thenReturn("cachedValue");

        Object result = dalAspect.aroundCacheable(pjp);

        assertEquals("cachedValue", result);
        verify(pjp, never()).proceed();
    }

    @Test
    void aroundCacheable_cacheMiss_executesAndCaches() throws Throwable {
        Method method = TestService.class.getMethod("cachedMethod", String.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"1"});
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"userId"});
        when(signature.getReturnType()).thenReturn((Class) String.class);
        when(cache.get("cache:user:1")).thenReturn(null);
        when(pjp.proceed()).thenReturn("computedValue");
        when(serializer.serialize("computedValue")).thenReturn("\"computedValue\"");

        Object result = dalAspect.aroundCacheable(pjp);

        assertEquals("computedValue", result);
        verify(cache).set("cache:user:1", "\"computedValue\"", 3600);
    }

    // ==================== Helper class ====================

    static class TestService {
        @RateLimit(key = "rate:user:#userId", permits = 1)
        public String rateLimitedMethod(String userId) {
            return "result";
        }

        @Idempotent(key = "idempot:order:#orderNo", window = 60)
        public String idempotentMethod(String orderNo) {
            return "result";
        }

        @Locker(key = "lock:order:#orderId", ttl = 30)
        public String lockedMethod(Long orderId) {
            return "result";
        }

        @Cacheable(key = "cache:user:#userId", ttl = 3600)
        public String cachedMethod(String userId) {
            return "computedValue";
        }
    }
}