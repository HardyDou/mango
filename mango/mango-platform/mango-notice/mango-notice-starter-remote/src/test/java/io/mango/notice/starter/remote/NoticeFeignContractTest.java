package io.mango.notice.starter.remote;

import io.mango.notice.api.NoticeApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

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

class NoticeFeignContractTest {

    @Test
    void feignClientImplementsThePublicApi() {
        assertThat(NoticeApi.class.isAssignableFrom(NoticeFeignClient.class)).isTrue();
    }

    @Test
    void feignEndpointsKeepVerbsPathsAndBindings() {
        assertThat(feignFingerprint()).isEqualTo("e9477ea905e555ddccd8919eaa19f478c3a4747918778b3e2746daaa8e02677f");
    }

    private static String feignFingerprint() {
        StringBuilder contract = new StringBuilder();
        Arrays.stream(NoticeFeignClient.class.getDeclaredMethods())
                .filter(method -> !method.isBridge() && !method.isSynthetic())
                .filter(method -> mapping(method) != null)
                .sorted(Comparator.comparing(NoticeFeignContractTest::methodSortKey))
                .forEach(method -> {
                    contract.append(verb(method)).append(' ').append(methodPath(method)).append(' ')
                            .append(method.getName()).append('(');
                    for (Parameter parameter : method.getParameters()) {
                        contract.append(parameter.getParameterizedType().getTypeName()).append('[');
                        Arrays.stream(parameter.getAnnotations())
                                .filter(NoticeFeignContractTest::isContractAnnotation)
                                .sorted(Comparator.comparing(annotation -> annotation.annotationType().getName()))
                                .forEach(annotation -> contract.append(annotationContract(annotation)).append(','));
                        contract.append("];");
                    }
                    contract.append(") -> ").append(method.getGenericReturnType().getTypeName()).append('\n');
                });
        return sha256(contract.toString());
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
