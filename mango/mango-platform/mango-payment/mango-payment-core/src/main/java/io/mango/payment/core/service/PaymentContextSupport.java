package io.mango.payment.core.service;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import org.springframework.util.StringUtils;

public final class PaymentContextSupport {

    private PaymentContextSupport() {
    }

    public static Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前机构上下文");
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            return Require.fail(400, "当前机构上下文不是有效数字: " + tenantId);
        }
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
