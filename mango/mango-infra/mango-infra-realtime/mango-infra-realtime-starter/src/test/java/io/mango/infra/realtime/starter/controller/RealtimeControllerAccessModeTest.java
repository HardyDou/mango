package io.mango.infra.realtime.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeControllerAccessModeTest {

    @Test
    void transportEndpointsShouldUseLoginAccess() throws Exception {
        assertAccessMode(RealtimeNegotiationController.class, "negotiate", ApiResourceAccessMode.LOGIN);
        assertAccessMode(SseRealtimeController.class, "connect", ApiResourceAccessMode.LOGIN);
        assertAccessMode(SseRealtimeController.class, "probe", ApiResourceAccessMode.LOGIN);
        assertAccessMode(SseRealtimeController.class, "inbound", ApiResourceAccessMode.LOGIN);
        assertAccessMode(PollingRealtimeController.class, "poll", ApiResourceAccessMode.LOGIN);
        assertAccessMode(PollingRealtimeController.class, "inbound", ApiResourceAccessMode.LOGIN);
        assertAccessMode(PollingRealtimeController.class, "probe", ApiResourceAccessMode.LOGIN);
    }

    @Test
    void serverToServerEndpointsShouldUseInternalAccess() {
        assertClassAccessMode(RealtimeApiController.class, ApiResourceAccessMode.INTERNAL);
        assertClassAccessMode(RealtimeInboundReceiverController.class, ApiResourceAccessMode.INTERNAL);
        assertClassAccessMode(RealtimeOutboundController.class, ApiResourceAccessMode.INTERNAL);
    }

    private static void assertAccessMode(Class<?> controllerClass, String methodName, ApiResourceAccessMode accessMode)
            throws Exception {
        Method method = findMethod(controllerClass, methodName);
        ApiAccess apiAccess = method.getAnnotation(ApiAccess.class);
        assertThat(apiAccess)
                .as(controllerClass.getSimpleName() + "#" + methodName + " should declare ApiAccess")
                .isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(accessMode);
    }

    private static void assertClassAccessMode(Class<?> controllerClass, ApiResourceAccessMode accessMode) {
        ApiAccess apiAccess = controllerClass.getAnnotation(ApiAccess.class);
        assertThat(apiAccess)
                .as(controllerClass.getSimpleName() + " should declare ApiAccess")
                .isNotNull();
        assertThat(apiAccess.mode()).isEqualTo(accessMode);
    }

    private static Method findMethod(Class<?> controllerClass, String methodName) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Method not found: " + controllerClass.getSimpleName() + "#" + methodName);
    }
}
