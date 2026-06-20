package io.mango.calendar.core.support;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import org.springframework.util.StringUtils;

public final class CalendarSupport {

    private CalendarSupport() {
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

    public static String trimRequired(String value, String message) {
        Require.notBlank(value, message);
        return value.trim();
    }

    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
