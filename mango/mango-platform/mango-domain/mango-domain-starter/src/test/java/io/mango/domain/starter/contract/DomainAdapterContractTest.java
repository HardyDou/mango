package io.mango.domain.starter.contract;

import io.mango.domain.api.DomainApi;
import io.mango.domain.starter.controller.DomainController;
import io.mango.domain.starter.remote.DomainFeignClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DomainAdapterContractTest {

    @Test
    void apiControllerFeign_公开方法签名完全一致() {
        Set<String> apiMethods = publicMethodSignatures(DomainApi.class);

        assertThat(publicMethodSignatures(DomainController.class)).isEqualTo(apiMethods);
        assertThat(publicMethodSignatures(DomainFeignClient.class)).isEqualTo(apiMethods);
    }

    @Test
    void controller和Feign_查询对象与简单参数使用明确且兼容的传输绑定() {
        assertParameterAnnotation(DomainController.class, "page", 0, org.springdoc.core.annotations.ParameterObject.class);
        assertParameterAnnotation(DomainController.class, "tree", 0, org.springdoc.core.annotations.ParameterObject.class);
        assertParameterAnnotation(DomainFeignClient.class, "page", 0, SpringQueryMap.class);
        assertParameterAnnotation(DomainFeignClient.class, "tree", 0, SpringQueryMap.class);

        for (String methodName : Set.of("detail", "detailByCode", "delete")) {
            assertParameterAnnotation(DomainController.class, methodName, 0, RequestParam.class);
            assertParameterAnnotation(DomainFeignClient.class, methodName, 0, RequestParam.class);
        }
        for (String methodName : Set.of("create", "update", "updateStatus")) {
            assertParameterAnnotation(DomainController.class, methodName, 0, RequestBody.class);
            assertParameterAnnotation(DomainFeignClient.class, methodName, 0, RequestBody.class);
        }
    }

    @Test
    void api单一声明参数校验_Controller和Feign不得重复约束() {
        Map<String, Class<? extends Annotation>> expected = Map.of(
                "page", Valid.class,
                "tree", Valid.class,
                "detail", NotNull.class,
                "detailByCode", NotBlank.class,
                "create", Valid.class,
                "update", Valid.class,
                "updateStatus", Valid.class,
                "delete", NotNull.class);

        expected.forEach((methodName, annotation) -> {
            assertParameterAnnotation(DomainApi.class, methodName, 0, annotation);
            assertParameterAnnotationAbsent(DomainController.class, methodName, 0, annotation);
            assertParameterAnnotationAbsent(DomainFeignClient.class, methodName, 0, annotation);
        });
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

    private void assertParameterAnnotation(
            Class<?> type, String methodName, int parameterIndex, Class<? extends Annotation> annotation) {
        Method method = methodsByName(type).get(methodName);
        assertThat(method).as(type.getSimpleName() + "#" + methodName).isNotNull();
        Parameter parameter = method.getParameters()[parameterIndex];
        assertThat(parameter.isAnnotationPresent(annotation))
                .as(type.getSimpleName() + "#" + methodName + " parameter requires @" + annotation.getSimpleName())
                .isTrue();
    }

    private void assertParameterAnnotationAbsent(
            Class<?> type, String methodName, int parameterIndex, Class<? extends Annotation> annotation) {
        Method method = methodsByName(type).get(methodName);
        assertThat(method).as(type.getSimpleName() + "#" + methodName).isNotNull();
        Parameter parameter = method.getParameters()[parameterIndex];
        assertThat(parameter.isAnnotationPresent(annotation))
                .as(type.getSimpleName() + "#" + methodName + " must inherit @" + annotation.getSimpleName())
                .isFalse();
    }

    private Map<String, Method> methodsByName(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .collect(Collectors.toMap(Method::getName, Function.identity()));
    }
}
