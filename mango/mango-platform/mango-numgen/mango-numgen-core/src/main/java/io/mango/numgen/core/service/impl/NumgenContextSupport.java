package io.mango.numgen.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.numgen.api.enums.NumgenCode;
import org.springframework.util.StringUtils;

final class NumgenContextSupport {

    private NumgenContextSupport() {
    }

    static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, NumgenCode.NUMGEN_TENANT_CONTEXT_INVALID, "缺少当前机构上下文");
        Require.isTrue(tenantId.chars().allMatch(Character::isDigit),
                NumgenCode.NUMGEN_TENANT_CONTEXT_INVALID, "当前机构上下文不是有效数字: " + tenantId);
        return tenantId;
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
