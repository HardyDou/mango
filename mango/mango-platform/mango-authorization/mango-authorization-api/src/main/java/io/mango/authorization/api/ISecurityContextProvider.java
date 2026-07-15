package io.mango.authorization.api;

import io.mango.authorization.api.vo.SecurityContextVO;

/**
 * 提供当前安全上下文，同时避免暴露平台业务模型。
 */
public interface ISecurityContextProvider {

    /**
     * 返回当前安全上下文。
     *
     * @return 安全上下文；未认证时返回 {@link SecurityContextVO#anonymous()}
     */
    SecurityContextVO currentContext();
}
