package io.mango.cms.starter.remote;

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
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CmsRemoteAdapterContractTest {

    private static final List<ApiFeignPair> PAIRS = List.of(
            pair(CmsContentCategoryApi.class, CmsContentCategoryFeignClient.class, "/cms"),
            pair(CmsContentTagApi.class, CmsContentTagFeignClient.class, "/cms"),
            pair(CmsSiteAdminApi.class, CmsSiteAdminFeignClient.class, "/cms"),
            pair(CmsSiteCategoryApi.class, CmsSiteCategoryFeignClient.class, "/cms"),
            pair(CmsContentApi.class, CmsContentFeignClient.class, "/cms"),
            pair(CmsContentPublishApi.class, CmsContentPublishFeignClient.class, "/cms"),
            pair(CmsNavigationApi.class, CmsNavigationFeignClient.class, "/cms"),
            pair(CmsBannerApi.class, CmsBannerFeignClient.class, "/cms"),
            pair(CmsAdvertisementApi.class, CmsAdvertisementFeignClient.class, "/cms"),
            pair(CmsAdDeliveryApi.class, CmsAdDeliveryFeignClient.class, "/cms"),
            pair(CmsSiteSettingApi.class, CmsSiteSettingFeignClient.class, "/cms"),
            pair(CmsSiteApi.class, CmsSiteFeignClient.class, "/cms/open"));

    private static final Set<Class<? extends Annotation>> HTTP_MAPPINGS = Set.of(
            GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class);

    @Test
    void remoteAdapters_完整覆盖十二个能力Api() {
        assertThat(PAIRS).hasSize(12);
        assertThat(PAIRS.stream().mapToInt(pair -> pair.feignType().getDeclaredMethods().length).sum())
                .isEqualTo(74);
        for (ApiFeignPair pair : PAIRS) {
            assertThat(pair.feignType().getInterfaces())
                    .as(pair.feignType().getSimpleName())
                    .containsExactly(pair.apiType());
            assertThat(methodKeys(pair.feignType()))
                    .as(pair.feignType().getSimpleName())
                    .containsExactlyInAnyOrderElementsOf(methodKeys(pair.apiType()));
        }
    }

    @Test
    void remoteAdapters_服务名上下文和根路径稳定() {
        assertThat(PAIRS.stream().map(pair -> pair.feignType().getAnnotation(FeignClient.class).contextId()))
                .doesNotHaveDuplicates();
        for (ApiFeignPair pair : PAIRS) {
            FeignClient annotation = pair.feignType().getAnnotation(FeignClient.class);
            assertThat(annotation).as(pair.feignType().getSimpleName()).isNotNull();
            assertThat(annotation.name()).isEqualTo("mango-cms");
            assertThat(annotation.path()).isEqualTo(pair.rootPath());
        }
    }

    @Test
    void remoteAdapters_每个方法都有单一Http映射和显式参数绑定() {
        PAIRS.stream()
                .flatMap(pair -> Arrays.stream(pair.feignType().getDeclaredMethods()))
                .forEach(method -> {
                    long mappingCount = Arrays.stream(method.getAnnotations())
                            .map(Annotation::annotationType)
                            .filter(HTTP_MAPPINGS::contains)
                            .count();
                    assertThat(mappingCount).as(method.toGenericString()).isOne();
                    Arrays.stream(method.getParameters()).forEach(CmsRemoteAdapterContractTest::assertBinding);
                });
    }

    @Test
    void criticalRemoteRoutes_remainStable() throws NoSuchMethodException {
        Method approve = CmsContentFeignClient.class.getDeclaredMethod(
                "approveContent", io.mango.cms.api.command.UpdateCmsContentReviewCommand.class);
        assertThat(approve.getAnnotation(PostMapping.class).value()).containsExactly("/contents/approve");

        Method resolve = CmsSiteFeignClient.class.getDeclaredMethod(
                "resolveSite", io.mango.cms.api.query.SiteResolveQuery.class);
        assertThat(resolve.getAnnotation(GetMapping.class).value()).containsExactly("/sites/resolve");
    }

    private static void assertBinding(Parameter parameter) {
        if (parameter.getType().getSimpleName().endsWith("Query")) {
            assertThat(parameter.getAnnotation(SpringQueryMap.class)).as(parameter.toString()).isNotNull();
        } else if (parameter.getType().equals(Long.class)) {
            assertThat(parameter.getAnnotation(RequestParam.class)).as(parameter.toString()).isNotNull();
        } else {
            assertThat(parameter.getAnnotation(RequestBody.class)).as(parameter.toString()).isNotNull();
        }
    }

    private static Set<String> methodKeys(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(CmsRemoteAdapterContractTest::methodKey)
                .collect(Collectors.toSet());
    }

    private static String methodKey(Method method) {
        return method.getName() + ':' + method.getGenericReturnType().getTypeName()
                + Arrays.toString(method.getGenericParameterTypes());
    }

    private static ApiFeignPair pair(Class<?> apiType, Class<?> feignType, String rootPath) {
        return new ApiFeignPair(apiType, feignType, rootPath);
    }

    private record ApiFeignPair(Class<?> apiType, Class<?> feignType, String rootPath) {
    }
}
