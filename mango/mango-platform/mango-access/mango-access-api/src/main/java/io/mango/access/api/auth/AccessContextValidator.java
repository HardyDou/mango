package io.mango.access.api.auth;

import io.mango.access.api.vo.AccessContextValidationResultVO;
import io.mango.access.api.vo.AccessPrincipalVO;

/**
 * 访问入口登录上下文校验扩展点。
 */
public interface AccessContextValidator {

    AccessContextValidationResultVO validate(AccessPrincipalVO principal);
}
