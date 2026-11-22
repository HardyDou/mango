package io.mango.authorization.data.grant;

/**
 * {@code @DataGrant} 端点配置错误。
 *
 * <p>典型场景：详情端点带注解，但既没有同名请求参数、也没有同名路径变量，且不是分页端点，
 * 导致记录主键无法解析。此时必须 fail-closed（返回服务端错误），不能静默放行，否则等于没有数据权限校验。</p>
 */
class DataGrantConfigurationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造异常。
     *
     * @param message 错误说明，需包含类名与方法名便于定位。
     */
    DataGrantConfigurationException(String message) {
        super(message);
    }
}
