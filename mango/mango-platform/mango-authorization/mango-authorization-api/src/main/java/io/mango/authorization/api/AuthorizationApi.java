package io.mango.authorization.api;

import io.mango.common.result.R;

/**
 * 授权查询远程契约。
 */
public interface AuthorizationApi {

    R<AuthorizationSnapshot> loadUserAuthorization(Long subjectId, String tenantId, String systemCode);
}
