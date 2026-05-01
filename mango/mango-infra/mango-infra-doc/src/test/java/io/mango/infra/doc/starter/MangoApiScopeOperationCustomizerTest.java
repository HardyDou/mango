package io.mango.infra.doc.starter;

import io.mango.authorization.api.annotation.InternalApi;
import io.mango.authorization.api.annotation.PermissionAccess;
import io.mango.authorization.api.annotation.PublicApi;
import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(publicOperation.getTags().contains("对外接口"));
        assertEquals(
                MangoApiScopeOperationCustomizer.EXTERNAL_SCOPE,
                permissionOperation.getExtensions().get(MangoApiScopeOperationCustomizer.SCOPE_EXTENSION));
        assertTrue(permissionOperation.getTags().contains("对外接口"));
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    static class TestController {

        @InternalApi
        void internal() {
        }

        @PublicApi
        void publicApi() {
        }

        @PermissionAccess("doc:test")
        void permission() {
        }
    }
}
