package io.mango.home.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.home.api.HomeOptionApi;
import io.mango.home.api.query.HomeUserOptionQuery;
import io.mango.home.api.vo.HomeUserOptionVO;
import io.mango.home.core.service.IHomeOptionService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeOptionControllerContractTest {

    @Test
    void userOptionEndpointsUseHomePermissionsAndDelegateToHomeService() throws NoSuchMethodException {
        IHomeOptionService service = mock(IHomeOptionService.class);
        HomeOptionController controller = new HomeOptionController(service);
        HomeUserOptionQuery query = new HomeUserOptionQuery();
        HomeUserOptionVO option = new HomeUserOptionVO();
        when(service.listPageUserOptions(query)).thenReturn(List.of(option));
        when(service.listVisibleUserOptions(query)).thenReturn(List.of(option));

        assertThat(controller).isInstanceOf(HomeOptionApi.class);
        assertThat(controller.listPageUserOptions(query).getData()).containsExactly(option);
        assertThat(controller.listVisibleUserOptions(query).getData()).containsExactly(option);
        verify(service).listPageUserOptions(same(query));
        verify(service).listVisibleUserOptions(same(query));
        assertPermission("listPageUserOptions", "home:list:view");
        assertPermission("listVisibleUserOptions", "home:user:view");
    }

    private void assertPermission(String methodName, String permission) throws NoSuchMethodException {
        Method method = HomeOptionController.class.getDeclaredMethod(methodName, HomeUserOptionQuery.class);
        ApiAccess access = method.getAnnotation(ApiAccess.class);
        assertThat(access).isNotNull();
        assertThat(access.mode()).isEqualTo(ApiResourceAccessMode.PERMISSION);
        assertThat(access.permission()).isEqualTo(permission);
    }
}
