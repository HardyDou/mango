package io.mango.auth.starter.remote;

import io.mango.auth.api.AuthApi;
import io.mango.auth.starter.controller.AuthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("认证本地与远程适配器契约测试")
class AuthAdapterContractTest {

    @Test
    @DisplayName("API、Controller 与 Feign 应暴露完全相同的方法签名")
    void adaptersShouldExposeTheSameApiSurface() {
        Set<String> apiMethods = publicMethodSignatures(AuthApi.class);

        assertThat(publicMethodSignatures(AuthController.class)).isEqualTo(apiMethods);
        assertThat(publicMethodSignatures(AuthFeignClient.class)).isEqualTo(apiMethods);
    }

    private Set<String> publicMethodSignatures(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(this::signature)
                .collect(Collectors.toSet());
    }

    private String signature(Method method) {
        return method.getName() + Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(",", "(", ")"));
    }
}
