package io.mango.identity.starter.controller;

import io.mango.identity.api.AuthIdentityApi;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.query.IdentityUserBatchQuery;
import io.mango.identity.api.TenantMemberApi;
import io.mango.identity.starter.remote.AuthIdentityFeignClient;
import io.mango.identity.starter.remote.IdentityUserFeignClient;
import io.mango.identity.starter.remote.TenantMemberFeignClient;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityAdapterContractTest {

    @Test
    void controllersAndFeignClientsShouldImplementEveryApiMethod() {
        assertAdapterContract(AuthIdentityApi.class, AuthIdentityController.class, AuthIdentityFeignClient.class);
        assertAdapterContract(IdentityUserApi.class, IdentityUserController.class, IdentityUserFeignClient.class);
        assertAdapterContract(TenantMemberApi.class, TenantMemberController.class, TenantMemberFeignClient.class);
    }

    @Test
    void controllersShouldInheritApiValidationWithoutRedeclaringIt() {
        assertValidationInheritance(AuthIdentityApi.class, AuthIdentityController.class);
        assertValidationInheritance(IdentityUserApi.class, IdentityUserController.class);
        assertValidationInheritance(TenantMemberApi.class, TenantMemberController.class);
    }

    @Test
    void batchIdentityLookupShouldKeepHttpContractAcrossAdapters() throws NoSuchMethodException {
        Method apiMethod = IdentityUserApi.class.getMethod("listUserInfos", IdentityUserBatchQuery.class);
        Method controllerMethod = IdentityUserController.class.getMethod("listUserInfos", IdentityUserBatchQuery.class);
        Method feignMethod = IdentityUserFeignClient.class.getMethod("listUserInfos", IdentityUserBatchQuery.class);

        assertThat(controllerMethod.getAnnotation(PostMapping.class).value())
                .containsExactly("/user/info/batch");
        assertThat(feignMethod.getAnnotation(PostMapping.class).value())
                .containsExactly("/user/info/batch");
        assertThat(controllerMethod.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(feignMethod.getParameters()[0].isAnnotationPresent(RequestBody.class)).isTrue();
        assertThat(apiMethod.getParameterTypes()).containsExactly(IdentityUserBatchQuery.class);
    }

    @Test
    void batchIdentityLookupShouldRequireLoginWithoutPermissionCode() throws NoSuchMethodException {
        Method controllerMethod = IdentityUserController.class.getMethod("listUserInfos", IdentityUserBatchQuery.class);
        ApiAccess access = controllerMethod.getAnnotation(ApiAccess.class);

        assertThat(access).isNotNull();
        assertThat(access.mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(access.permission()).isBlank();
    }

    @Test
    void batchIdentityQueryShouldDefensivelyCopyIdentifiers() {
        List<Long> userIds = new ArrayList<>(List.of(1001L));
        List<String> usernames = new ArrayList<>(List.of("admin"));
        IdentityUserBatchQuery query = new IdentityUserBatchQuery();

        query.setUserIds(userIds);
        query.setUsernames(usernames);
        userIds.add(1002L);
        usernames.add("reviewer");

        assertThat(query.getUserIds()).containsExactly(1001L);
        assertThat(query.getUsernames()).containsExactly("admin");
        query.getUserIds().add(1003L);
        query.getUsernames().add("operator");
        assertThat(query.getUserIds()).containsExactly(1001L);
        assertThat(query.getUsernames()).containsExactly("admin");
    }

    private void assertAdapterContract(Class<?> api, Class<?> controller, Class<?> feignClient) {
        assertThat(api.isAssignableFrom(controller)).isTrue();
        assertThat(api.isAssignableFrom(feignClient)).isTrue();
        for (Method method : api.getDeclaredMethods()) {
            assertThat(findMethod(controller, method)).isNotNull();
            assertThat(findMethod(feignClient, method)).isNotNull();
        }
    }

    private void assertValidationInheritance(Class<?> api, Class<?> controller) {
        assertThat(controller.isAnnotationPresent(Validated.class)).isTrue();
        for (Method apiMethod : api.getDeclaredMethods()) {
            Method controllerMethod = findMethod(controller, apiMethod);
            for (Parameter parameter : controllerMethod.getParameters()) {
                assertThat(parameter.isAnnotationPresent(Valid.class)).isFalse();
            }
        }
    }

    private Method findMethod(Class<?> type, Method contractMethod) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(contractMethod.getName()))
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), contractMethod.getParameterTypes()))
                .findFirst()
                .orElse(null);
    }
}
