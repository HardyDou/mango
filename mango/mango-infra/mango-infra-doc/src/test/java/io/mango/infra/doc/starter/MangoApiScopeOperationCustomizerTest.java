package io.mango.infra.doc.starter;

import io.mango.authorization.api.annotation.InternalAccess;
import io.mango.authorization.api.annotation.PermissionAccess;
import io.mango.authorization.api.annotation.PublicAccess;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MangoApiScopeOperationCustomizerTest {

    private final MangoApiScopeOperationCustomizer customizer = new MangoApiScopeOperationCustomizer();

    @Test
    void shouldMarkInternalApiAsInternalScope() throws NoSuchMethodException {
        Operation operation = customizer.customize(new Operation(), handlerMethod("internal"));

        assertEquals(
                MangoApiScopeOperationCustomizer.INTERNAL_SCOPE,
                operation.getExtensions().get(MangoApiScopeOperationCustomizer.SCOPE_EXTENSION));
        assertTrue(operation.getTags().contains("对内接口"));
    }

    @Test
    void shouldMarkOtherAccessModesAsExternalScope() throws NoSuchMethodException {
        Operation publicOperation = customizer.customize(new Operation(), handlerMethod("publicApi"));
        Operation permissionOperation = customizer.customize(new Operation(), handlerMethod("permission"));

        assertEquals(
                MangoApiScopeOperationCustomizer.EXTERNAL_SCOPE,
                publicOperation.getExtensions().get(MangoApiScopeOperationCustomizer.SCOPE_EXTENSION));
        assertFalse(publicOperation.getTags() != null && publicOperation.getTags().contains("对外接口"));
        assertEquals(
                MangoApiScopeOperationCustomizer.EXTERNAL_SCOPE,
                permissionOperation.getExtensions().get(MangoApiScopeOperationCustomizer.SCOPE_EXTENSION));
        assertFalse(permissionOperation.getTags() != null && permissionOperation.getTags().contains("对外接口"));
    }

    @Test
    void shouldAttachBearerAuthToNonPublicApis() throws NoSuchMethodException {
        Operation publicOperation = customizer.customize(new Operation(), handlerMethod("publicApi"));
        Operation permissionOperation = customizer.customize(new Operation(), handlerMethod("permission"));
        Operation loginOperation = customizer.customize(new Operation(), handlerMethod("login"));

        assertTrue(publicOperation.getSecurity() == null || publicOperation.getSecurity().isEmpty());
        assertTrue(permissionOperation.getSecurity().stream()
                .anyMatch(requirement -> requirement.containsKey(MangoApiScopeOperationCustomizer.AUTHORIZATION_HEADER_SCHEME)));
        assertTrue(loginOperation.getSecurity().stream()
                .anyMatch(requirement -> requirement.containsKey(MangoApiScopeOperationCustomizer.AUTHORIZATION_HEADER_SCHEME)));
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    static class TestController {

        @InternalAccess
        void internal() {
        }

        @PublicAccess
        void publicApi() {
        }

        @PermissionAccess("doc:test")
        void permission() {
        }

        void login() {
        }
    }
}
