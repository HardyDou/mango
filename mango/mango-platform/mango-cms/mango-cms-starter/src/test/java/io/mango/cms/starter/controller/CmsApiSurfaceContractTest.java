package io.mango.cms.starter.controller;

import io.mango.cms.api.CmsAdminApi;
import io.mango.cms.api.CmsSiteApi;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class CmsApiSurfaceContractTest {

    @Test
    void adminApi_keepsItsCompleteJavaContract() {
        assertThat(contractFingerprint(CmsAdminApi.class))
                .isEqualTo("a932efdaa71a3ac3e16014ca0db9984d3921f7f96bd5f4f6e4e51ac9a1edd473");
    }

    @Test
    void publicSiteApi_keepsItsCompleteJavaContract() {
        assertThat(contractFingerprint(CmsSiteApi.class))
                .isEqualTo("f3b47db53ba377390133ca7186b6fc00e28d448b4426a010ce4fbfb0e598652b");
    }

    private static String contractFingerprint(Class<?> apiType) {
        StringBuilder contract = new StringBuilder(apiType.getName()).append('\n');
        Arrays.stream(apiType.getAnnotations())
                .map(CmsApiSurfaceContractTest::annotationContract)
                .sorted()
                .forEach(value -> contract.append("TYPE ").append(value).append('\n'));
        Arrays.stream(apiType.getDeclaredMethods())
                .sorted(Comparator.comparing(CmsApiSurfaceContractTest::methodSortKey))
                .forEach(method -> appendMethod(contract, method));
        return sha256(contract.toString());
    }

    private static void appendMethod(StringBuilder contract, Method method) {
        contract.append("METHOD ")
                .append(method.getName())
                .append(" -> ")
                .append(method.getGenericReturnType().getTypeName())
                .append('\n');
        for (Parameter parameter : method.getParameters()) {
            contract.append("PARAM ").append(parameter.getParameterizedType().getTypeName());
            Arrays.stream(parameter.getAnnotations())
                    .map(CmsApiSurfaceContractTest::annotationContract)
                    .sorted()
                    .forEach(value -> contract.append(' ').append(value));
            contract.append('\n');
        }
    }

    private static String methodSortKey(Method method) {
        return method.getName() + Arrays.toString(method.getGenericParameterTypes());
    }

    private static String annotationContract(Annotation annotation) {
        return annotation.annotationType().getName() + ':' + annotation;
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
