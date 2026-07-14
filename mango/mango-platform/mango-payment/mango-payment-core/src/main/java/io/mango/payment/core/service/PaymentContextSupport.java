package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.payment.api.enums.PaymentCode;
import org.springframework.util.StringUtils;

public final class PaymentContextSupport {

    private PaymentContextSupport() {
    }

    public static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID, "缺少当前机构上下文");
        return tenantId.trim();
    }

    public static Long currentUserId() {
        return MangoContextHolder.userId();
    }

    public static String currentPrincipalName() {
        String principalName = MangoContextHolder.principalName();
        return StringUtils.hasText(principalName) ? principalName.trim() : "system";
    }

    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
