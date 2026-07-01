package io.mango.link.core.support;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import org.springframework.util.StringUtils;

public final class LinkContextSupport {

    private LinkContextSupport() {
    }

    public static Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前机构上下文");
        return parseLong(tenantId, "当前机构上下文不是有效数字: " + tenantId);
    }

    public static Long currentTenantIdOrNull() {
        String tenantId = MangoContextHolder.tenantId();
        return StringUtils.hasText(tenantId) ? parseLong(tenantId, "当前机构上下文不是有效数字: " + tenantId) : null;
    }

    public static Long currentUserId() {
        Long userId = MangoContextHolder.userId();
        Require.notNull(userId, "请先登录");
        return userId;
    }

    public static Long currentUserIdOrNull() {
        return MangoContextHolder.userId();
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

    public static Long resolveTenantId(Long tenantId) {
        Long currentTenantId = currentTenantIdOrNull();
        if (currentTenantId != null) {
            return currentTenantId;
        }
        Require.notNull(tenantId, "租户上下文不能为空");
        return tenantId;
    }

    private static Long parseLong(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return Require.fail(400, message);
        }
    }
}
