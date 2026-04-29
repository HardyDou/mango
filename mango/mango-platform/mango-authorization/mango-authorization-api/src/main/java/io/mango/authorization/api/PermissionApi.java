package io.mango.authorization.api;

import io.mango.common.result.R;

import java.util.Set;

/**
 * 权限码管理 API 契约。
 */
public interface PermissionApi {

    /**
     * 查询全部权限码。
     *
     * @return 权限码集合
     */
    R<Set<String>> getAllPermissionCodes();
}
