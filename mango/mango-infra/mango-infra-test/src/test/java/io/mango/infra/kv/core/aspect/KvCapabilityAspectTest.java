package io.mango.infra.kv.core.aspect;

import io.mango.common.exception.BizException;
import io.mango.infra.kv.api.expression.KvContextContributor;
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
        KvContextContributor contributor = context ->
                context.setVariable("req", new ReqVariables(
                        Map.of("X-Tenant", "tenant-a"),
                        Map.of("SESSION", "session-1")));
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
    void resolveKey_templateExpression_supportsMethodArgsBeanAndExpressionContext() throws Exception {
        String key = resolveKey("user:#{@tenantKey.prefix()}:#{#req.headers['X-Tenant']}:#{#req.cookies['SESSION']}:#{#userId}");

        assertEquals("user:tenant-prefix:tenant-a:session-1:u1", key);
    }

    @Test
    void resolveKey_directExpression_supportsMethodArg() throws Exception {
        String key = resolveKey("#userId");

        assertEquals("u1", key);
    }

    @Test
    void resolveKey_templateExpression_supportsObjectMethodArgProperties() throws Exception {
        Method method = SampleService.class.getMethod("findByQuery", UserQuery.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"query"});
        ProceedingJoinPoint objectJoinPoint = mock(ProceedingJoinPoint.class);
        when(objectJoinPoint.getSignature()).thenReturn(signature);
        when(objectJoinPoint.getArgs()).thenReturn(new Object[]{new UserQuery("tenant-a", "u1")});

        String key = resolveKey("user:#{#query.tenantId}:#{#query.userId}", objectJoinPoint);

        assertEquals("user:tenant-a:u1", key);
    }

    @Test
    void resolveKey_inlineTokenWithoutTemplate_rejected() {
        BizException exception = assertThrows(BizException.class, () -> resolveKey("user:#userId"));

        assertTrue(exception.getMessage().contains("SpEL template syntax"));
    }

    private String resolveKey(String expression) throws Exception {
        return resolveKey(expression, joinPoint);
    }

    private String resolveKey(String expression, ProceedingJoinPoint targetJoinPoint) throws Exception {
        Method resolveKey = KvCapabilityAspect.class.getDeclaredMethod("resolveKey", String.class, ProceedingJoinPoint.class);
        resolveKey.setAccessible(true);
        try {
            return (String) resolveKey.invoke(aspect, expression, targetJoinPoint);
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

        public String findByQuery(UserQuery query) {
            return query.userId();
        }
    }

    public static class TenantKey {
        public String prefix() {
            return "tenant-prefix";
        }
    }

    public record ReqVariables(Map<String, String> headers, Map<String, String> cookies) {
    }

    public record UserQuery(String tenantId, String userId) {
    }
}
