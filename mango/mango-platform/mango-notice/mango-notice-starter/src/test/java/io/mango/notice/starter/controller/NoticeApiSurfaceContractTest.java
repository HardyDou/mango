package io.mango.notice.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.notice.api.NoticeAnnouncementApi;
import io.mango.notice.api.NoticeApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeApiSurfaceContractTest {

    private static final List<Class<?>> PUBLIC_APIS = List.of(NoticeApi.class, NoticeAnnouncementApi.class);
    private static final List<Class<?>> PUBLIC_CONTROLLERS = List.of(
            NoticeController.class, NoticeAnnouncementController.class);

    @Test
    void publicApisKeepMethodsParametersValidationAndReturns() {
        assertThat(apiFingerprint()).isEqualTo("8ed5782740c8a93aba3627484a61f21e2c846713360732e2a74e43086bbae470");
    }

    @Test
    void httpEndpointsKeepVerbsPathsBindingsReturnsAndPermissions() {
        assertThat(httpFingerprint()).isEqualTo("4ea31ab91737e718cccc1b00db3442a9c2a6280047174e60edf5c83c48e1b976");
    }

    @Test
    void controllersImplementThePublicApis() {
        assertThat(NoticeApi.class.isAssignableFrom(NoticeController.class)).isTrue();
        assertThat(NoticeAnnouncementApi.class.isAssignableFrom(NoticeAnnouncementController.class)).isTrue();
    }

    private static String apiFingerprint() {
        StringBuilder contract = new StringBuilder();
        PUBLIC_APIS.stream()
                .sorted(Comparator.comparing(Class::getName))
                .forEach(api -> {
                    contract.append("API ").append(api.getName()).append('\n');
                    Arrays.stream(api.getDeclaredMethods())
                            .filter(method -> !method.isBridge() && !method.isSynthetic())
                            .sorted(Comparator.comparing(NoticeApiSurfaceContractTest::methodSortKey))
                            .forEach(method -> appendMethod(contract, method));
                });
        return sha256(contract.toString());
    }

    private static String httpFingerprint() {
        StringBuilder contract = new StringBuilder();
        PUBLIC_CONTROLLERS.stream()
                .sorted(Comparator.comparing(Class::getName))
                .forEach(controller -> appendController(contract, controller));
        return sha256(contract.toString());
    }

    private static void appendController(StringBuilder contract, Class<?> controller) {
        RequestMapping root = controller.getAnnotation(RequestMapping.class);
        String rootPath = root == null ? "" : firstPath(root.path(), root.value());
        contract.append("CONTROLLER ").append(controller.getName()).append(' ').append(rootPath).append('\n');
        Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> !method.isBridge() && !method.isSynthetic())
                .filter(method -> mapping(method) != null)
                .sorted(Comparator.comparing(NoticeApiSurfaceContractTest::methodSortKey))
                .forEach(method -> {
                    contract.append(verb(method)).append(' ')
                            .append(rootPath).append(methodPath(method)).append('\n');
                    appendMethod(contract, method);
                    ApiAccess access = method.getAnnotation(ApiAccess.class);
                    if (access != null) {
                        contract.append("ACCESS ").append(access.mode()).append(' ')
                                .append(access.permission()).append(' ')
                                .append(access.desc()).append('\n');
                    }
                });
    }

    private static void appendMethod(StringBuilder contract, Method method) {
        contract.append(method.getName()).append('(');
        for (Parameter parameter : method.getParameters()) {
            contract.append(parameter.getParameterizedType().getTypeName()).append('[');
            Arrays.stream(parameter.getAnnotations())
                    .filter(NoticeApiSurfaceContractTest::isContractAnnotation)
                    .sorted(Comparator.comparing(annotation -> annotation.annotationType().getName()))
                    .forEach(annotation -> contract.append(annotationContract(annotation)).append(','));
            contract.append("];");
        }
        contract.append(") -> ").append(method.getGenericReturnType().getTypeName()).append('\n');
    }

    private static String verb(Method method) {
        Annotation annotation = mapping(method);
        if (annotation instanceof GetMapping) {
            return "GET";
        }
        if (annotation instanceof PostMapping) {
            return "POST";
        }
        if (annotation instanceof PutMapping) {
            return "PUT";
        }
        if (annotation instanceof PatchMapping) {
            return "PATCH";
        }
        if (annotation instanceof DeleteMapping) {
            return "DELETE";
        }
        throw new IllegalStateException("Unsupported mapping: " + annotation);
    }

    private static String methodPath(Method method) {
        Annotation annotation = mapping(method);
        if (annotation instanceof GetMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof PostMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof PutMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof PatchMapping value) {
            return firstPath(value.path(), value.value());
        }
        if (annotation instanceof DeleteMapping value) {
            return firstPath(value.path(), value.value());
        }
        return "";
    }

    private static Annotation mapping(Method method) {
        for (Class<? extends Annotation> type : List.of(
                GetMapping.class, PostMapping.class, PutMapping.class, PatchMapping.class, DeleteMapping.class)) {
            Annotation annotation = method.getAnnotation(type);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

    private static String firstPath(String[] path, String[] value) {
        if (path.length > 0) {
            return path[0];
        }
        if (value.length > 0) {
            return value[0];
        }
        return "";
    }

    private static String methodSortKey(Method method) {
        return method.getName() + Arrays.toString(method.getGenericParameterTypes())
                + method.getGenericReturnType().getTypeName();
    }

    private static String annotationContract(Annotation annotation) {
        StringBuilder contract = new StringBuilder(annotation.annotationType().getName()).append('(');
        Arrays.stream(annotation.annotationType().getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .forEach(method -> {
                    try {
                        contract.append(method.getName()).append('=')
                                .append(Arrays.deepToString(new Object[]{method.invoke(annotation)})).append(';');
                    } catch (ReflectiveOperationException ex) {
                        throw new IllegalStateException("Cannot read annotation " + annotation, ex);
                    }
                });
        return contract.append(')').toString();
    }

    private static boolean isContractAnnotation(Annotation annotation) {
        String name = annotation.annotationType().getName();
        return name.startsWith("org.springframework.web.bind.annotation.")
                || name.startsWith("jakarta.validation.");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
