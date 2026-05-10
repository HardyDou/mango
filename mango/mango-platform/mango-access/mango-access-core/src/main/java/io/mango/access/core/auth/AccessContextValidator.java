package io.mango.access.core.auth;

/**
 * 访问入口登录上下文校验扩展点。
 * <p>
 * access-core 不依赖具体业务模块，机构状态、成员状态等运行时事实由业务模块实现。
 */
public interface AccessContextValidator {

    AccessContextValidationResult validate(AccessPrincipal principal);
}
