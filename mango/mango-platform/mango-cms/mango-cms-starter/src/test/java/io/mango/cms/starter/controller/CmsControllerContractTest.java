package io.mango.cms.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
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
    void cmsAdminController_管理接口显式声明权限或访问模式() {
        for (Method method : CmsAdminController.class.getDeclaredMethods()) {
            if (!method.getName().startsWith("lambda$")) {
                assertThat(method.getAnnotation(ApiAccess.class))
                        .as(method.getName())
                        .isNotNull();
            }
        }
    }
}
