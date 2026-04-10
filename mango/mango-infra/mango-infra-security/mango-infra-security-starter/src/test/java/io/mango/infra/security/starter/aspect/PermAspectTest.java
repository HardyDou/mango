package io.mango.infra.security.starter.aspect;

import io.mango.infra.security.api.Perm;
import io.mango.common.exception.BizException;
import io.mango.infra.security.api.IPermissionService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for PermAspect
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PermAspect Tests")
class PermAspectTest {

    private PermAspect permAspect;

    @Mock
    private IPermissionService permissionService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() throws Exception {
        permAspect = new PermAspect();
        injectField(permAspect, "permissionService", permissionService);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("permissionService == null should skip check and not throw")
    void checkPermission_nullPermissionService_noException() throws Exception {
        injectField(permAspect, "permissionService", null);

        Method method = TestController.class.getMethod("testMethod");
        when(methodSignature.getMethod()).thenReturn(method);

        // Should not throw
        assertDoesNotThrow(() -> permAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("userId == null should skip check and not throw")
    void checkPermission_nullUserId_noException() throws Exception {
        Method method = TestController.class.getMethod("testMethod");
        when(methodSignature.getMethod()).thenReturn(method);

        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getAttribute("userId")).thenReturn(null);
        RequestContextHolder.setRequestAttributes(attributes);

        // Should not throw
        assertDoesNotThrow(() -> permAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("User has permission should not throw")
    void checkPermission_userHasPermission_noException() throws Exception {
        Method method = TestController.class.getMethod("testMethod");
        when(methodSignature.getMethod()).thenReturn(method);

        when(permissionService.listUserPermissions(anyLong()))
                .thenReturn(List.of("user:test:add", "user:test:view"));

        setUserIdAttribute(1L);

        // Should not throw
        assertDoesNotThrow(() -> permAspect.checkPermission(joinPoint));
    }

    @Test
    @DisplayName("User lacks permission should throw BizException")
    void checkPermission_userLacksPermission_throwsBizException() throws Exception {
        Method method = TestController.class.getMethod("testMethod");
        when(methodSignature.getMethod()).thenReturn(method);

        when(permissionService.listUserPermissions(anyLong()))
                .thenReturn(List.of("user:other:view"));

        setUserIdAttribute(1L);

        BizException exception = assertThrows(BizException.class,
                () -> permAspect.checkPermission(joinPoint));

        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("没有访问该资源的权限"));
    }

    @Test
    @DisplayName("Integer userId should be converted to Long")
    void checkPermission_integerUserId_works() throws Exception {
        Method method = TestController.class.getMethod("testMethod");
        when(methodSignature.getMethod()).thenReturn(method);

        when(permissionService.listUserPermissions(42L))
                .thenReturn(List.of("user:test:add"));

        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getAttribute("userId")).thenReturn(42); // Integer, not Long
        RequestContextHolder.setRequestAttributes(attributes);

        assertDoesNotThrow(() -> permAspect.checkPermission(joinPoint));
        verify(permissionService).listUserPermissions(42L);
    }

    private void setUserIdAttribute(Long userId) {
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        when(request.getAttribute("userId")).thenReturn(userId);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /** Test controller for method resolution */
    static class TestController {
        @Perm("user:test:add")
        public void testMethod() {
        }
    }
}
