package io.mango.system.starter.controller;

import io.mango.area.api.SysAreaApi;
import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.annotation.PublicAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.area.core.service.ISysAreaService;
import io.mango.system.api.AdminBrandingApi;
import io.mango.system.api.DictApi;
import io.mango.system.api.SysConfigApi;
import io.mango.system.api.SysLoginLogApi;
import io.mango.system.api.SysOperationLogApi;
import io.mango.system.api.SysTenantApi;
import io.mango.system.api.query.LoginLogPageQuery;
import io.mango.system.core.service.IDictService;
import io.mango.system.core.service.ISysConfigService;
import io.mango.system.core.service.ISysLogService;
import io.mango.system.core.service.ISysTenantService;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SystemApiContractTest {

    @Test
    void systemApi_只由Controller承载() {
        assertThat(DictApi.class).isAssignableFrom(SysDictController.class);
        assertThat(SysTenantApi.class).isAssignableFrom(SysTenantController.class);
        assertThat(SysConfigApi.class).isAssignableFrom(SysConfigController.class);
        assertThat(SysLoginLogApi.class).isAssignableFrom(SysLoginLogController.class);
        assertThat(SysOperationLogApi.class).isAssignableFrom(SysOperationLogController.class);
        assertThat(SysAreaApi.class).isAssignableFrom(SysAreaController.class);
        assertThat(AdminBrandingApi.class).isAssignableFrom(AdminBrandingController.class);

        assertThat(DictApi.class.isAssignableFrom(IDictService.class)).isFalse();
        assertThat(SysTenantApi.class.isAssignableFrom(ISysTenantService.class)).isFalse();
        assertThat(SysConfigApi.class.isAssignableFrom(ISysConfigService.class)).isFalse();
        assertThat(SysLoginLogApi.class.isAssignableFrom(ISysLogService.class)).isFalse();
        assertThat(SysOperationLogApi.class.isAssignableFrom(ISysLogService.class)).isFalse();
        assertThat(SysAreaApi.class.isAssignableFrom(ISysAreaService.class)).isFalse();
    }

    @Test
    void currentUserLoginLogsRequireLoginWithoutAdminPermission() throws Exception {
        Method method = SysLoginLogController.class.getMethod("pageCurrentUser", LoginLogPageQuery.class);

        ApiAccess access = method.getAnnotation(ApiAccess.class);
        assertThat(access.mode()).isEqualTo(ApiResourceAccessMode.LOGIN);
        assertThat(access.permission()).isEmpty();
    }

    @Test
    void websitePublicConfigUsesStandardPublicAccessContract() throws Exception {
        Method method = AdminBrandingController.class.getMethod("publicConfig");

        PublicAccess publicAccess = method.getAnnotation(PublicAccess.class);
        ApiAccess access = AnnotatedElementUtils.findMergedAnnotation(method, ApiAccess.class);

        assertThat(publicAccess).isNotNull();
        assertThat(publicAccess.desc()).isEqualTo("网站公共配置");
        assertThat(publicAccess.version()).isEqualTo(2);
        assertThat(access).isNotNull();
        assertThat(access.mode()).isEqualTo(ApiResourceAccessMode.PUBLIC);
        assertThat(access.permission()).isEmpty();
        assertThat(access.version()).isEqualTo(2);
    }
}
