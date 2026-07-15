package io.mango.gridlayout.core.service.impl;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;

import static io.mango.gridlayout.api.enums.GridLayoutCode.GRID_LAYOUT_INVALID;

final class GridLayoutContextSupport {

    private GridLayoutContextSupport() {
    }

    static Long currentUserId() {
        Long userId = MangoContextHolder.userId();
        Require.notNull(userId, GRID_LAYOUT_INVALID, "缺少当前用户上下文");
        return userId;
    }

    static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, GRID_LAYOUT_INVALID, "缺少当前租户上下文");
        return tenantId;
    }
}
