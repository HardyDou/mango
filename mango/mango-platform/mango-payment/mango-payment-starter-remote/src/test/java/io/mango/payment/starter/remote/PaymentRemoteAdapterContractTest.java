package io.mango.payment.starter.remote;

import io.mango.payment.api.PaymentSecurityApi;
import io.mango.payment.api.PaymentTaskApi;
import org.junit.jupiter.api.Test;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRemoteAdapterContractTest {

    private static final int PAYMENT_API_COUNT = 27;
    private static final int PAYMENT_METHOD_COUNT = 127;
    private static final Set<Class<? extends Annotation>> HTTP_MAPPINGS = Set.of(
            GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class, PatchMapping.class);

    @Test
    void remoteAdapters_coverAllPaymentApisAndMethods() throws ReflectiveOperationException {
        List<Class<?>> feignTypes = PaymentRemoteContractFixtures.feignTypes();
        assertThat(feignTypes).hasSize(PAYMENT_API_COUNT);
        assertThat(feignTypes.stream().mapToInt(type -> type.getDeclaredMethods().length).sum())
                .isEqualTo(PAYMENT_METHOD_COUNT);

        for (Class<?> feignType : feignTypes) {
            Class<?> apiType = PaymentRemoteContractFixtures.apiType(feignType);
            Class<?> controllerType = controllerType(apiType);
            assertThat(feignType.getInterfaces()).as(feignType.getSimpleName()).containsExactly(apiType);
            assertThat(apiType.isAssignableFrom(controllerType)).as(controllerType.getSimpleName()).isTrue();
            assertThat(methodKeys(feignType)).containsExactlyInAnyOrderElementsOf(methodKeys(apiType));
            assertThat(methodKeys(controllerType)).containsExactlyInAnyOrderElementsOf(methodKeys(apiType));
            assertMethodContracts(apiType, controllerType, feignType);
        }
    }

    @Test
    void remoteAdapters_usePaymentModuleTargetsAndUniqueContexts() throws ReflectiveOperationException {
        List<Class<?>> feignTypes = PaymentRemoteContractFixtures.feignTypes();
        assertThat(feignTypes.stream().map(type -> type.getAnnotation(FeignClient.class).contextId()))
                .doesNotHaveDuplicates();

        for (Class<?> feignType : feignTypes) {
            Class<?> controllerType = controllerType(PaymentRemoteContractFixtures.apiType(feignType));
            FeignClient feign = feignType.getAnnotation(FeignClient.class);
            String controllerPath = controllerType.getAnnotation(RequestMapping.class).value()[0];
            assertThat(feign).as(feignType.getSimpleName()).isNotNull();
            assertThat(feign.name()).isEqualTo("mango-payment");
            assertThat(feign.path()).isEqualTo(controllerPath);
            assertThat(feign.contextId()).isEqualTo(lowerCamel(feignType.getSimpleName()));
        }

        assertThat(PaymentRemoteContractFixtures.apiType(PaymentSecurityFeignClient.class))
                .isEqualTo(PaymentSecurityApi.class);
        assertThat(PaymentRemoteContractFixtures.apiType(PaymentTaskFeignClient.class))
                .isEqualTo(PaymentTaskApi.class);
        assertThat(PaymentOpenFeignClient.class.getAnnotation(FeignClient.class).path())
                .isEqualTo("/openapi/pay");
    }

    private static void assertMethodContracts(Class<?> apiType, Class<?> controllerType, Class<?> feignType)
            throws ReflectiveOperationException {
        for (Method apiMethod : apiType.getDeclaredMethods()) {
            Method controllerMethod = controllerType.getDeclaredMethod(apiMethod.getName(), apiMethod.getParameterTypes());
            Method feignMethod = feignType.getDeclaredMethod(apiMethod.getName(), apiMethod.getParameterTypes());
            assertThat(feignMethod.getGenericReturnType()).isEqualTo(apiMethod.getGenericReturnType());
            assertThat(controllerMethod.getGenericReturnType()).isEqualTo(apiMethod.getGenericReturnType());
            assertHttpMapping(controllerMethod, feignMethod);
            assertParameterBindings(controllerMethod, feignMethod);
        }
    }

    private static void assertHttpMapping(Method controllerMethod, Method feignMethod) {
        Annotation controllerMapping = singleHttpMapping(controllerMethod);
        Annotation feignMapping = singleHttpMapping(feignMethod);
        assertThat(feignMapping.annotationType()).isEqualTo(controllerMapping.annotationType());
        assertThat(mappingPath(feignMapping)).isEqualTo(mappingPath(controllerMapping));
    }

    private static Annotation singleHttpMapping(Method method) {
        List<Annotation> mappings = Arrays.stream(method.getAnnotations())
                .filter(annotation -> HTTP_MAPPINGS.contains(annotation.annotationType()))
                .toList();
        assertThat(mappings).as(method.toGenericString()).hasSize(1);
        return mappings.get(0);
    }

    private static String mappingPath(Annotation mapping) {
        try {
            String[] path = (String[]) mapping.annotationType().getMethod("path").invoke(mapping);
            String[] value = (String[]) mapping.annotationType().getMethod("value").invoke(mapping);
            if (path.length > 0) {
                return path[0];
            }
            return value.length == 0 ? "" : value[0];
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read HTTP mapping path", exception);
        }
    }

    private static void assertParameterBindings(Method controllerMethod, Method feignMethod) {
        Parameter[] controllerParameters = controllerMethod.getParameters();
        Parameter[] feignParameters = feignMethod.getParameters();
        assertThat(feignParameters).hasSameSizeAs(controllerParameters);
        for (int index = 0; index < controllerParameters.length; index++) {
            Parameter controllerParameter = controllerParameters[index];
            Parameter feignParameter = feignParameters[index];
            if (controllerParameter.isAnnotationPresent(ParameterObject.class)) {
                assertThat(feignParameter.isAnnotationPresent(SpringQueryMap.class)).isTrue();
                continue;
            }
            if (controllerParameter.isAnnotationPresent(RequestBody.class)) {
                assertThat(feignParameter.isAnnotationPresent(RequestBody.class)).isTrue();
                continue;
            }
            RequestParam controllerRequestParam = controllerParameter.getAnnotation(RequestParam.class);
            RequestParam feignRequestParam = feignParameter.getAnnotation(RequestParam.class);
            assertThat(feignRequestParam).as(feignParameter.toString()).isNotNull();
            assertThat(feignRequestParam.name()).isEqualTo(controllerRequestParam.name());
            assertThat(feignRequestParam.value()).isEqualTo(controllerRequestParam.value());
            assertThat(feignRequestParam.required()).isEqualTo(controllerRequestParam.required());
            assertThat(feignRequestParam.defaultValue()).isEqualTo(controllerRequestParam.defaultValue());
        }
    }

    private static Class<?> controllerType(Class<?> apiType) throws ClassNotFoundException {
        String packageName = "io.mango.payment.starter.controller.";
        String apiName = apiType.getSimpleName();
        for (String candidate : List.of(apiName + "Controller", apiName.substring(0, apiName.length() - 3) + "Controller")) {
            try {
                return Class.forName(packageName + candidate);
            } catch (ClassNotFoundException ignored) {
                // Try the standard XxxController form after XxxApiController.
            }
        }
        throw new ClassNotFoundException("No Payment controller for " + apiName);
    }

    private static Set<String> methodKeys(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !method.isBridge() && !method.isSynthetic())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(PaymentRemoteAdapterContractTest::methodKey)
                .collect(Collectors.toSet());
    }

    private static String methodKey(Method method) {
        return method.getName() + ':' + method.getGenericReturnType().getTypeName()
                + Arrays.toString(method.getGenericParameterTypes());
    }

    private static String lowerCamel(String value) {
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
