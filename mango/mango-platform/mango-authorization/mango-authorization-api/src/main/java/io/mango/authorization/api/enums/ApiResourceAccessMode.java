package io.mango.authorization.api.enums;

/**
 * API 资源访问模式。
 *
 * @author hardy
 */
public enum ApiResourceAccessMode {

    /**
     * 匿名访问。
     */
    PUBLIC,

    /**
     * 需要登录，不需要权限码。
     */
    LOGIN,

    /**
     * 需要登录且需要权限码。
     */
    PERMISSION,

    /**
     * 仅允许内部调用。
     */
    INTERNAL
}
