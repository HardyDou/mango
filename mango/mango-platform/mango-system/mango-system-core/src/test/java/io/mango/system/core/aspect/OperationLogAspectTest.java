package io.mango.system.core.aspect;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.log.annotation.Log;
import io.mango.system.api.po.SysOperationLogPo;
import io.mango.system.core.service.ISysLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogAspectTest {

    private final ISysLogService logService = mock(ISysLogService.class);
    private final OperationLogAspect aspect = new OperationLogAspect(logService, null);

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        MangoContextHolder.clear();
    }

    @Test
    void skipsPublicApiWithoutExplicitLog() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/tenant/login-options");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint point = joinPoint("publicApi");

        Object result = aspect.around(point);

        assertThat(result).isEqualTo("ok");
        verify(logService, never()).recordOperationLog(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void filtersServletArgumentsWhenRecordingOperationLog() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/config");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(1001L, "1", "admin", null, null, null, null, null));
        ProceedingJoinPoint point = joinPoint("loggedApi", request, response, new DemoCommand("siteName"));

        aspect.around(point);

        ArgumentCaptor<SysOperationLogPo> captor = ArgumentCaptor.forClass(SysOperationLogPo.class);
        verify(logService).recordOperationLog(captor.capture());
        SysOperationLogPo log = captor.getValue();
        assertThat(log.getTenantId()).isEqualTo(1L);
        assertThat(log.getOperation()).isEqualTo("修改配置");
        assertThat(log.getParams())
                .contains("[ServletRequest]")
                .contains("[ServletResponse]")
                .contains("siteName");
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object... args) throws Throwable {
        Method method = DemoController.class.getDeclaredMethod(methodName, parameterTypes(args));
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringTypeName()).thenReturn(DemoController.class.getName());
        when(signature.getDeclaringType()).thenReturn((Class) DemoController.class);
        when(signature.getName()).thenReturn(methodName);

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(args);
        when(point.proceed()).thenReturn("ok");
        return point;
    }

    private Class<?>[] parameterTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        return types;
    }

    static class DemoController {

        @ApiAccess(mode = ApiResourceAccessMode.PUBLIC)
        String publicApi() {
            return "ok";
        }

        @Log("修改配置")
        String loggedApi(MockHttpServletRequest request, MockHttpServletResponse response, DemoCommand command) {
            return "ok";
        }
    }

    record DemoCommand(String name) {
    }
}
