package io.mango.home.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;

final class HomeContextSupport {

    private static final String INTERNAL_ORG_PARTY = "INTERNAL_ORG";

    private HomeContextSupport() {
    }

    static Long currentUserId() {
        Long userId = MangoContextHolder.userId();
        Require.notNull(userId, "缺少当前用户上下文");
        return userId;
    }

    static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "缺少当前租户上下文");
        return tenantId;
    }

    static Long currentOrgId() {
        if (INTERNAL_ORG_PARTY.equals(MangoContextHolder.get().partyType())) {
            return MangoContextHolder.get().partyId();
        }
        return null;
    }
}
