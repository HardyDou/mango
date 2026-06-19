package io.mango.access.api.auth;

/**
 * 访问入口登录上下文校验扩展点。
 */
public interface AccessContextValidator {

    AccessContextValidationResult validate(AccessPrincipal principal);
}
