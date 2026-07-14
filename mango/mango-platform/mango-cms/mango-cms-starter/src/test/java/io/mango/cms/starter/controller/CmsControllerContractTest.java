package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.cms.api.CmsAdminApi;
import io.mango.cms.api.CmsSiteApi;
import io.mango.cms.core.service.ICmsContentService;
import io.mango.cms.core.service.ICmsSiteService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CmsControllerContractTest {

    @Test
    void cmsControllers_不使用路径变量() {
        Stream.of(CmsAdminController.class, CmsSiteController.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
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
    void cmsApi_只由Controller承载() {
        assertThat(CmsAdminApi.class).isAssignableFrom(CmsAdminController.class);
        assertThat(CmsSiteApi.class).isAssignableFrom(CmsSiteController.class);
        assertThat(CmsAdminApi.class.isAssignableFrom(ICmsContentService.class)).isFalse();
        assertThat(CmsSiteApi.class.isAssignableFrom(ICmsSiteService.class)).isFalse();
    }

    @Test
    void cmsAdminController_管理接口显式声明权限或访问模式() {
        for (Method method : CmsAdminController.class.getDeclaredMethods()) {
            if (!method.getName().startsWith("lambda$")) {
                assertThat(method.getAnnotation(ApiAccess.class))
                        .as(method.getName())
                        .isNotNull();
            }
        }
    }

    @Test
    void cmsControllers_完整实现两个公开Api方法集合() {
        assertThat(apiMethodKeys(CmsAdminController.class, CmsAdminApi.class))
                .containsExactlyInAnyOrderElementsOf(apiMethodKeys(CmsAdminApi.class, CmsAdminApi.class));
        assertThat(apiMethodKeys(CmsSiteController.class, CmsSiteApi.class))
                .containsExactlyInAnyOrderElementsOf(apiMethodKeys(CmsSiteApi.class, CmsSiteApi.class));
    }

    @Test
    void criticalHttpRoutesAndPermissions_remainStable() throws NoSuchMethodException {
        Method approve = CmsAdminController.class.getDeclaredMethod(
                "approveContent", io.mango.cms.api.command.UpdateCmsContentReviewCommand.class);
        assertThat(approve.getAnnotation(PostMapping.class).value()).containsExactly("/contents/approve");
        assertThat(approve.getAnnotation(ApiAccess.class).permission()).isEqualTo("cms:content:approve");

        Method resolve = CmsSiteController.class.getDeclaredMethod(
                "resolveSite", io.mango.cms.api.query.SiteResolveQuery.class);
        assertThat(resolve.getAnnotation(GetMapping.class).value()).containsExactly("/sites/resolve");

    }

    private static Set<String> apiMethodKeys(Class<?> implementation, Class<?> apiType) {
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
}
