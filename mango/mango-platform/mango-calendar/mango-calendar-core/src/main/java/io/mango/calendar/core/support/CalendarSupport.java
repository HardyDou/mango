package io.mango.calendar.core.support;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import org.springframework.util.StringUtils;

import static io.mango.calendar.api.enums.CalendarCode.CALENDAR_BUSINESS_ERROR;

public final class CalendarSupport {

    private CalendarSupport() {
    }

    public static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, CALENDAR_BUSINESS_ERROR, "缺少当前机构上下文");
        return tenantId;
    }

    public static String trimRequired(String value, String message) {
        Require.notBlank(value, CALENDAR_BUSINESS_ERROR, message);
        return value.trim();
    }

    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
