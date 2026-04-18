package io.mango.infra.kv.core.aspect;

import io.mango.common.spi.request.RequestContextContributor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KvCapabilityAspectTest {

    private KvCapabilityAspect aspect;
    private ProceedingJoinPoint joinPoint;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() throws NoSuchMethodException {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("tenantKey", new TenantKey());
        RequestContextContributor contributor = context -> {
            context.setAttribute("headers", Map.of("X-Tenant", "tenant-a"));
            context.setAttribute("cookies", Map.of("SESSION", "session-1"));
        };
        ObjectProvider provider = mock(ObjectProvider.class);
        aspect = new KvCapabilityAspect(provider, provider, provider, provider, provider, beanFactory, List.of(contributor));

        Method method = SampleService.class.getMethod("find", String.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"userId"});
        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"u1"});
    }

    @Test
    void resolveKey_templateExpression_supportsMethodArgsBeanAndRequestContext() throws Exception {
        String key = resolveKey("user:#{@tenantKey.prefix()}:#{#headers['X-Tenant']}:#{#cookies['SESSION']}:#{#userId}");

        assertEquals("user:tenant-prefix:tenant-a:session-1:u1", key);
    }

    @Test
    void resolveKey_directExpression_supportsMethodArg() throws Exception {
        String key = resolveKey("#userId");

        assertEquals("u1", key);
    }

    @Test
    void resolveKey_inlineTokenWithoutTemplate_rejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> resolveKey("user:#userId"));

        assertTrue(exception.getMessage().contains("SpEL template syntax"));
    }

    private String resolveKey(String expression) throws Exception {
        Method resolveKey = KvCapabilityAspect.class.getDeclaredMethod("resolveKey", String.class, ProceedingJoinPoint.class);
        resolveKey.setAccessible(true);
        try {
            return (String) resolveKey.invoke(aspect, expression, joinPoint);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ex;
        }
    }

    public static class SampleService {
        public String find(String userId) {
            return userId;
        }
    }

    public static class TenantKey {
        public String prefix() {
            return "tenant-prefix";
        }
    }
}
