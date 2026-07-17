package io.mango.link.core.support;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.link.api.enums.LinkCode;
import org.springframework.util.StringUtils;

public final class LinkContextSupport {

    private LinkContextSupport() {
    }

    public static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, LinkCode.LINK_BUSINESS_ERROR, "缺少当前机构上下文");
        return tenantId;
    }

    public static String currentTenantIdOrNull() {
        String tenantId = MangoContextHolder.tenantId();
        return StringUtils.hasText(tenantId) ? tenantId : null;
    }

    public static Long currentUserId() {
        Long userId = MangoContextHolder.userId();
        Require.notNull(userId, LinkCode.LINK_BUSINESS_ERROR, "请先登录");
        return userId;
    }

    public static Long currentUserIdOrNull() {
        return MangoContextHolder.userId();
    }

    public static String trimRequired(String value, String message) {
        Require.notBlank(value, LinkCode.LINK_BUSINESS_ERROR, message);
        return value.trim();
    }

    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public static String resolveTenantId(Long tenantId) {
        String currentTenantId = currentTenantIdOrNull();
        if (currentTenantId != null) {
            return currentTenantId;
        }
        Require.notNull(tenantId, LinkCode.LINK_BUSINESS_ERROR, "租户上下文不能为空");
        return String.valueOf(tenantId);
    }
}
