package io.mango.cms.starter.controller;

import io.mango.cms.api.CmsAdDeliveryApi;
import io.mango.cms.api.CmsAdvertisementApi;
import io.mango.cms.api.CmsBannerApi;
import io.mango.cms.api.CmsContentApi;
import io.mango.cms.api.CmsContentCategoryApi;
import io.mango.cms.api.CmsContentPublishApi;
import io.mango.cms.api.CmsContentTagApi;
import io.mango.cms.api.CmsNavigationApi;
import io.mango.cms.api.CmsSiteAdminApi;
import io.mango.cms.api.CmsSiteApi;
import io.mango.cms.api.CmsSiteCategoryApi;
import io.mango.cms.api.CmsSiteSettingApi;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CmsApiSurfaceContractTest {

    private static final List<Class<?>> MANAGEMENT_APIS = List.of(
            CmsContentCategoryApi.class,
            CmsContentTagApi.class,
            CmsSiteAdminApi.class,
            CmsSiteCategoryApi.class,
            CmsContentApi.class,
            CmsContentPublishApi.class,
            CmsNavigationApi.class,
            CmsBannerApi.class,
            CmsAdvertisementApi.class,
            CmsAdDeliveryApi.class,
            CmsSiteSettingApi.class);

    @Test
    void splitManagementApis_keepCompleteLegacyJavaContract() {
        assertThat(MANAGEMENT_APIS.stream().mapToInt(type -> type.getDeclaredMethods().length).sum())
                .isEqualTo(66);
        assertThat(contractFingerprint(MANAGEMENT_APIS, false))
                .isEqualTo("27344edbc29ac735186a866a189b66db3eb75e994709a1169e1a9f9e4a037d06");
    }

    @Test
    void publicSiteApi_keepsItsCompleteJavaContract() {
        assertThat(contractFingerprint(List.of(CmsSiteApi.class), true))
                .isEqualTo("f3b47db53ba377390133ca7186b6fc00e28d448b4426a010ce4fbfb0e598652b");
    }

    private static String contractFingerprint(List<Class<?>> apiTypes, boolean includeTypeContract) {
        StringBuilder contract = new StringBuilder();
        if (includeTypeContract) {
            Class<?> apiType = apiTypes.get(0);
            contract.append(apiType.getName()).append('\n');
            Arrays.stream(apiType.getAnnotations())
                    .map(CmsApiSurfaceContractTest::annotationContract)
                    .sorted()
                    .forEach(value -> contract.append("TYPE ").append(value).append('\n'));
        }
        List<Method> methods = new ArrayList<>();
        apiTypes.forEach(type -> methods.addAll(Arrays.asList(type.getDeclaredMethods())));
        methods.stream()
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
