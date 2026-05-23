package io.mango.numgen.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.context.core.MangoContextHolder;
import org.springframework.util.StringUtils;

final class NumgenContextSupport {

    private NumgenContextSupport() {
    }

    static Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前机构上下文");
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            return Require.fail(400, "当前机构上下文不是有效数字: " + tenantId);
        }
    }

    static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    static String trimToBlank(String value) {
        return value == null ? "" : value.trim();
    }
}
