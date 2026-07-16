package io.mango.identity.starter.controller;

import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.TenantMemberApi;
import io.mango.identity.starter.remote.AuthIdentityFeignClient;
import io.mango.identity.starter.remote.IdentityUserFeignClient;
import io.mango.identity.starter.remote.TenantMemberFeignClient;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAdapterContractTest {

    @Test
    void controllersAndFeignClientsShouldImplementEveryApiMethod() {
        assertAdapterContract(AuthIdentityApi.class, AuthIdentityController.class, AuthIdentityFeignClient.class);
        assertAdapterContract(IdentityUserApi.class, IdentityUserController.class, IdentityUserFeignClient.class);
        assertAdapterContract(TenantMemberApi.class, TenantMemberController.class, TenantMemberFeignClient.class);
    }

    @Test
    void controllersShouldInheritApiValidationWithoutRedeclaringIt() {
        assertValidationInheritance(AuthIdentityApi.class, AuthIdentityController.class);
        assertValidationInheritance(IdentityUserApi.class, IdentityUserController.class);
        assertValidationInheritance(TenantMemberApi.class, TenantMemberController.class);
    }

    private void assertAdapterContract(Class<?> api, Class<?> controller, Class<?> feignClient) {
        assertThat(api.isAssignableFrom(controller)).isTrue();
        assertThat(api.isAssignableFrom(feignClient)).isTrue();
        for (Method method : api.getDeclaredMethods()) {
            assertThat(findMethod(controller, method)).isNotNull();
            assertThat(findMethod(feignClient, method)).isNotNull();
        }
    }

    private void assertValidationInheritance(Class<?> api, Class<?> controller) {
        assertThat(controller.isAnnotationPresent(Validated.class)).isTrue();
        for (Method apiMethod : api.getDeclaredMethods()) {
            Method controllerMethod = findMethod(controller, apiMethod);
            for (Parameter parameter : controllerMethod.getParameters()) {
                assertThat(parameter.isAnnotationPresent(Valid.class)).isFalse();
            }
        }
    }

    private Method findMethod(Class<?> type, Method contractMethod) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(contractMethod.getName()))
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), contractMethod.getParameterTypes()))
                .findFirst()
                .orElse(null);
    }
}
