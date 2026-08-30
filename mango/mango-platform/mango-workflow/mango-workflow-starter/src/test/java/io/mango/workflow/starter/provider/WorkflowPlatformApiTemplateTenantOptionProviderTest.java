package io.mango.workflow.starter.provider;

import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.system.api.SysTenantApi;
import io.mango.system.api.vo.SysTenantVO;
import io.mango.workflow.api.WorkflowTemplateTenantOptionProvider;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.starter.WorkflowAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowPlatformApiTemplateTenantOptionProviderTest {

    @Test
    void optionsReturnsOnlyEnabledMatchingTenantFields() {
        SysTenantApi tenantApi = mock(SysTenantApi.class);
        when(tenantApi.list()).thenReturn(R.ok(List.of(
                tenant(1L, "默认机构", "default", 1),
                tenant(2L, "停用机构", "disabled", 0),
                tenant(3L, "华东机构", "east", 1))));

        var options = provider(tenantApi).options(" 华东 ");

        assertThat(options).singleElement().satisfies(option -> assertThat(option)
                .extracting("id", "tenantName", "tenantCode")
                .containsExactly(3L, "华东机构", "east"));
    }

    @Test
    void optionsFailsExplicitlyWhenSystemApiIsMissingOrFails() {
        assertThatThrownBy(() -> provider(null).options(null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(WorkflowCode.TEMPLATE_TENANT_OPTION_PROVIDER_MISSING.getCode());

        SysTenantApi tenantApi = mock(SysTenantApi.class);
        when(tenantApi.list()).thenReturn(R.fail(500, "system unavailable"));
        assertThatThrownBy(() -> provider(tenantApi).options(null))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(WorkflowCode.TEMPLATE_TENANT_OPTION_LOAD_FAILED.getCode());
    }

    @Test
    void autoConfigurationAllowsApplicationsToReplaceTenantProvider() throws NoSuchMethodException {
        Method factory = WorkflowAutoConfiguration.class.getDeclaredMethod(
                "workflowTemplateTenantOptionProvider", ObjectProvider.class);
        ConditionalOnMissingBean condition = factory.getAnnotation(ConditionalOnMissingBean.class);

        assertThat(condition).isNotNull();
        assertThat(condition.value()).containsExactly(WorkflowTemplateTenantOptionProvider.class);
    }

    private WorkflowPlatformApiTemplateTenantOptionProvider provider(SysTenantApi api) {
        @SuppressWarnings("unchecked")
        ObjectProvider<SysTenantApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(api);
        return new WorkflowPlatformApiTemplateTenantOptionProvider(provider);
    }

    private SysTenantVO tenant(Long id, String name, String code, Integer status) {
        SysTenantVO tenant = new SysTenantVO();
        tenant.setId(id);
        tenant.setTenantName(name);
        tenant.setTenantCode(code);
        tenant.setStatus(status);
        return tenant;
    }
}
