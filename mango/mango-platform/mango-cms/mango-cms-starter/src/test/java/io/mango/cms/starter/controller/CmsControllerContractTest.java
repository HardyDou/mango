package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CmsControllerContractTest {

    private static final List<ApiControllerPair> MANAGEMENT_PAIRS = List.of(
            new ApiControllerPair(CmsContentCategoryApi.class, CmsContentCategoryController.class),
            new ApiControllerPair(CmsContentTagApi.class, CmsContentTagController.class),
            new ApiControllerPair(CmsSiteAdminApi.class, CmsSiteAdminController.class),
            new ApiControllerPair(CmsSiteCategoryApi.class, CmsSiteCategoryController.class),
            new ApiControllerPair(CmsContentApi.class, CmsContentController.class),
            new ApiControllerPair(CmsContentPublishApi.class, CmsContentPublishController.class),
            new ApiControllerPair(CmsNavigationApi.class, CmsNavigationController.class),
            new ApiControllerPair(CmsBannerApi.class, CmsBannerController.class),
            new ApiControllerPair(CmsAdvertisementApi.class, CmsAdvertisementController.class),
            new ApiControllerPair(CmsAdDeliveryApi.class, CmsAdDeliveryController.class),
            new ApiControllerPair(CmsSiteSettingApi.class, CmsSiteSettingController.class));

    private static final ApiControllerPair PUBLIC_SITE_PAIR =
            new ApiControllerPair(CmsSiteApi.class, CmsSiteController.class);

    @Test
    void cmsControllers_不使用路径变量() {
        allPairs().stream()
                .flatMap(pair -> Arrays.stream(pair.controllerType().getDeclaredMethods()))
                .flatMap(method -> Arrays.stream(method.getParameters()))
                .map(Parameter::getAnnotations)
                .flatMap(Arrays::stream)
                .forEach(annotation -> assertThat(annotation).isNotInstanceOf(PathVariable.class));
    }

    @Test
    void cmsSiteController_匿名公开访问() {
        ApiAccess access = CmsSiteController.class.getAnnotation(ApiAccess.class);

        assertThat(access).isNotNull();
        assertThat(access.mode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
    }

    @Test
    void cmsApis_每个只由一个Controller承载() {
        for (ApiControllerPair pair : allPairs()) {
            assertThat(pair.controllerType().getInterfaces())
                    .as(pair.controllerType().getSimpleName())
                    .containsExactly(pair.apiType());
        }
    }

    @Test
    void cmsManagementControllers_管理接口显式声明权限模式() {
        MANAGEMENT_PAIRS.stream()
                .flatMap(pair -> Arrays.stream(pair.controllerType().getDeclaredMethods()))
                .forEach(method -> assertThat(method.getAnnotation(ApiAccess.class))
                        .as(method.getDeclaringClass().getSimpleName() + '#' + method.getName())
                        .isNotNull());
    }

    @Test
    void cmsControllers_完整实现十二个能力Api方法集合() {
        for (ApiControllerPair pair : allPairs()) {
            assertThat(methodKeys(pair.controllerType(), pair.apiType()))
                    .as(pair.controllerType().getSimpleName())
                    .containsExactlyInAnyOrderElementsOf(methodKeys(pair.apiType(), pair.apiType()));
        }
    }

    @Test
    void cmsControllers_保持管理和公共根路径() {
        MANAGEMENT_PAIRS.forEach(pair -> assertThat(pair.controllerType().getAnnotation(RequestMapping.class).value())
                .containsExactly("/cms"));
        assertThat(CmsSiteController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/cms/open");
    }

    @Test
    void criticalHttpRoutesAndPermissions_remainStable() throws NoSuchMethodException {
        Method approve = CmsContentController.class.getDeclaredMethod(
                "approveContent", io.mango.cms.api.command.UpdateCmsContentReviewCommand.class);
        assertThat(approve.getAnnotation(PostMapping.class).value()).containsExactly("/contents/approve");
        assertThat(approve.getAnnotation(ApiAccess.class).permission()).isEqualTo("cms:content:approve");

        Method resolve = CmsSiteController.class.getDeclaredMethod(
                "resolveSite", io.mango.cms.api.query.SiteResolveQuery.class);
        assertThat(resolve.getAnnotation(GetMapping.class).value()).containsExactly("/sites/resolve");
    }

    private static List<ApiControllerPair> allPairs() {
        return java.util.stream.Stream.concat(MANAGEMENT_PAIRS.stream(), java.util.stream.Stream.of(PUBLIC_SITE_PAIR))
                .toList();
    }

    private static Set<String> methodKeys(Class<?> implementation, Class<?> apiType) {
        Set<String> apiNames = Arrays.stream(apiType.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        return Arrays.stream(implementation.getDeclaredMethods())
                .filter(method -> apiNames.contains(method.getName()))
                .map(CmsControllerContractTest::methodKey)
                .collect(Collectors.toSet());
    }

    private static String methodKey(Method method) {
        return method.getName() + Arrays.toString(Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .toArray(String[]::new));
    }

    private record ApiControllerPair(Class<?> apiType, Class<?> controllerType) {
    }
}
