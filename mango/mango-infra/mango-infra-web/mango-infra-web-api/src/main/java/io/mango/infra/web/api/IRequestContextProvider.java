package io.mango.infra.web.api;

/**
 * 以基础设施契约形式提供当前 HTTP 请求上下文。
 */
public interface IRequestContextProvider {

    /**
     * 返回当前 HTTP 请求上下文。
     *
     * @return 请求上下文；非 HTTP 请求场景返回 {@link RequestContextSnapshot#empty()}
     */
    RequestContextSnapshot currentContext();
}
