package io.mango.system.core.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.access.api.auth.AccessContextValidator;
import io.mango.access.api.vo.AccessContextValidationResultVO;
import io.mango.access.api.vo.AccessPrincipalVO;
import io.mango.system.api.enums.InstitutionStatus;
import io.mango.system.core.entity.SysTenantEntity;
import io.mango.system.core.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 访问入口机构上下文校验。
 */
@Component
@RequiredArgsConstructor
public class InstitutionAccessContextValidator implements AccessContextValidator {

    private final SysTenantMapper sysTenantMapper;

    @Override
    public AccessContextValidationResultVO validate(AccessPrincipalVO principal) {
        if (principal == null || principal.tenantId() == null || principal.tenantId().isBlank()) {
            return AccessContextValidationResultVO.allow();
        }
        Long tenantId = parseTenantId(principal.tenantId());
        if (tenantId == null) {
            return AccessContextValidationResultVO.deny("机构上下文非法，请重新登录");
        }
        SysTenantEntity tenant = sysTenantMapper.selectOne(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getId, tenantId)
                .last("LIMIT 1"));
        if (tenant == null) {
            return AccessContextValidationResultVO.deny("机构不存在，请重新登录");
        }
        InstitutionStatus status = InstitutionStatus.of(tenant.getStatus());
        if (status == null || !status.enabled()) {
            return AccessContextValidationResultVO.deny("当前机构已" + statusLabel(status) + "，请重新登录");
        }
        return AccessContextValidationResultVO.allow();
    }

    private Long parseTenantId(String tenantId) {
        try {
            return Long.valueOf(tenantId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String statusLabel(InstitutionStatus status) {
        return status == null ? "不可用" : status.label();
    }
}
