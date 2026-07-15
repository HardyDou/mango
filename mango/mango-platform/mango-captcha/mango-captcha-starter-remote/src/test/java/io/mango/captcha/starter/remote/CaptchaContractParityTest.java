package io.mango.captcha.starter.remote;

import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.captcha.starter.controller.CaptchaController;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.infra.web.api.Inner;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaContractParityTest {

    private static final Map<String, Endpoint> ENDPOINTS = Map.of(
            "getTypes", new Endpoint("GET", "/types", null),
            "generateArithmetic", new Endpoint("GET", "/arithmetic", null),
            "generateBlockPuzzle", new Endpoint("GET", "/block-puzzle", null),
            "generateClickWord", new Endpoint("GET", "/click-word", null),
            "generateBehavior", new Endpoint("GET", "/behavior", null),
            "verifyBehavior", new Endpoint("POST", "/behavior/verify", CaptchaVerifyRequest.class),
            "verify", new Endpoint("POST", "/verify", CaptchaVerifyRequest.class),
            "send", new Endpoint("POST", "/send", CaptchaSendRequest.class));

    @Test
    void apiControllerAndFeignKeepOneToOneMethodParity() throws ReflectiveOperationException {
        assertThat(CaptchaApi.class).isAssignableFrom(CaptchaController.class);
        assertThat(CaptchaApi.class).isAssignableFrom(CaptchaFeignClient.class);
        assertThat(CaptchaController.class.getAnnotation(RequestMapping.class).value()).containsExactly("/captcha");
        assertThat(CaptchaFeignClient.class.getAnnotation(FeignClient.class).path()).isEqualTo("/captcha");

        Map<String, Method> apiMethods = declaredMethods(CaptchaApi.class);
        Map<String, Method> controllerMethods = declaredMethods(CaptchaController.class);
        Map<String, Method> feignMethods = declaredMethods(CaptchaFeignClient.class);
        assertThat(apiMethods.keySet()).isEqualTo(ENDPOINTS.keySet());
        assertThat(controllerMethods.keySet()).isEqualTo(ENDPOINTS.keySet());
        assertThat(feignMethods.keySet()).isEqualTo(ENDPOINTS.keySet());

        for (Map.Entry<String, Endpoint> entry : ENDPOINTS.entrySet()) {
            Method apiMethod = apiMethods.get(entry.getKey());
            Method controllerMethod = controllerMethods.get(entry.getKey());
            Method feignMethod = feignMethods.get(entry.getKey());
            assertSignatureParity(apiMethod, controllerMethod);
            assertSignatureParity(apiMethod, feignMethod);
            assertEndpoint(controllerMethod, entry.getValue());
            assertEndpoint(feignMethod, entry.getValue());
            assertValidationOwnedByApi(apiMethod, controllerMethod, feignMethod);
        }
    }

    @Test
    void sendEndpointOverridesThePublicControllerAsInternalOnly() throws ReflectiveOperationException {
        Method apiMethod = CaptchaApi.class.getMethod("send", CaptchaSendRequest.class);
        Method controllerMethod = CaptchaController.class.getMethod("send", CaptchaSendRequest.class);

        assertThat(apiMethod.isAnnotationPresent(Inner.class)).isTrue();
        assertThat(controllerMethod.isAnnotationPresent(Inner.class)).isTrue();
        assertThat(controllerMethod.getAnnotation(ApiAccess.class).mode())
                .isEqualTo(ApiResourceAccessMode.INTERNAL);
    }

    private static Map<String, Method> declaredMethods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !method.isBridge() && !method.isSynthetic())
                .collect(Collectors.toMap(Method::getName, Function.identity()));
    }

    private static void assertSignatureParity(Method apiMethod, Method adapterMethod) {
        assertThat(adapterMethod.getGenericReturnType()).isEqualTo(apiMethod.getGenericReturnType());
        assertThat(adapterMethod.getGenericParameterTypes()).containsExactly(apiMethod.getGenericParameterTypes());
    }

    private static void assertEndpoint(Method method, Endpoint endpoint) {
        assertThat(httpVerb(method)).isEqualTo(endpoint.verb());
        assertThat(mappingPath(method)).isEqualTo(endpoint.path());
        if (endpoint.requestType() == null) {
            assertThat(method.getParameters()).isEmpty();
            return;
        }
        assertThat(method.getParameterTypes()).containsExactly(endpoint.requestType());
        Parameter parameter = method.getParameters()[0];
        assertThat(parameter.isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(parameter.isAnnotationPresent(Valid.class)).isFalse();
    }

    private static void assertValidationOwnedByApi(Method apiMethod, Method controllerMethod, Method feignMethod) {
        if (apiMethod.getParameterCount() == 0) {
            return;
        }
        assertThat(apiMethod.getParameters()[0].isAnnotationPresent(Valid.class)).isTrue();
        assertThat(controllerMethod.getParameters()[0].isAnnotationPresent(Valid.class)).isFalse();
        assertThat(feignMethod.getParameters()[0].isAnnotationPresent(Valid.class)).isFalse();
    }

    private static String httpVerb(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return "GET";
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return "POST";
        }
        return "";
    }

    private static String mappingPath(Method method) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            return firstPath(getMapping.path(), getMapping.value());
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            return firstPath(postMapping.path(), postMapping.value());
        }
        return "";
    }

    private static String firstPath(String[] path, String[] value) {
        if (path.length > 0) {
            return path[0];
        }
        return value[0];
    }

    private record Endpoint(String verb, String path, Class<?> requestType) {
    }
}
