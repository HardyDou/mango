package io.mango.workflow.starter.provider;

import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.system.api.SysTenantApi;
import io.mango.system.api.vo.SysTenantVO;
import io.mango.workflow.api.WorkflowTemplateTenantOptionProvider;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.vo.WorkflowTenantOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** 通过 System 公共 Java API 提供流程模板推送目标机构候选项。 */
@RequiredArgsConstructor
public class WorkflowPlatformApiTemplateTenantOptionProvider
        implements WorkflowTemplateTenantOptionProvider {

    private final ObjectProvider<SysTenantApi> sysTenantApiProvider;

    @Override
    public List<WorkflowTenantOptionVO> options(String keyword) {
        SysTenantApi tenantApi = Require.nonNull(sysTenantApiProvider.getIfAvailable(),
                WorkflowCode.TEMPLATE_TENANT_OPTION_PROVIDER_MISSING);
        R<List<SysTenantVO>> response = tenantApi.list();
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null,
                WorkflowCode.TEMPLATE_TENANT_OPTION_LOAD_FAILED);
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;
        return Objects.requireNonNull(response).getData().stream()
                .filter(tenant -> tenant.getId() != null && Integer.valueOf(1).equals(tenant.getStatus()))
                .filter(tenant -> matches(tenant, normalizedKeyword))
                .map(this::toOption)
                .toList();
    }

    private boolean matches(SysTenantVO tenant, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(tenant.getTenantName(), keyword) || contains(tenant.getTenantCode(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private WorkflowTenantOptionVO toOption(SysTenantVO tenant) {
        WorkflowTenantOptionVO option = new WorkflowTenantOptionVO();
        option.setId(tenant.getId());
        option.setTenantName(StringUtils.hasText(tenant.getTenantName())
                ? tenant.getTenantName().trim()
                : String.valueOf(tenant.getId()));
        option.setTenantCode(StringUtils.hasText(tenant.getTenantCode()) ? tenant.getTenantCode().trim() : null);
        return option;
    }
}
