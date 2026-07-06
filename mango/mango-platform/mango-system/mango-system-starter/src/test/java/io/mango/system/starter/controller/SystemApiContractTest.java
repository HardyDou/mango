package io.mango.system.starter.controller;

import io.mango.system.api.DictApi;
import io.mango.system.api.SysConfigApi;
import io.mango.system.api.SysTenantApi;
import io.mango.system.core.service.IDictService;
import io.mango.system.core.service.ISysConfigService;
import io.mango.system.core.service.ISysTenantService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemApiContractTest {

    @Test
    void systemApi_只由Controller承载() {
        assertThat(DictApi.class).isAssignableFrom(SysDictController.class);
        assertThat(SysTenantApi.class).isAssignableFrom(SysTenantController.class);
        assertThat(SysConfigApi.class).isAssignableFrom(SysConfigController.class);

        assertThat(DictApi.class.isAssignableFrom(IDictService.class)).isFalse();
        assertThat(SysTenantApi.class.isAssignableFrom(ISysTenantService.class)).isFalse();
        assertThat(SysConfigApi.class.isAssignableFrom(ISysConfigService.class)).isFalse();
    }
}
