package io.mango.infra.log.annotation;

/**
 * 日志类型。
 */
public enum LogType {
    /**
     * 登录。
     */
    LOGIN,
    /**
     * 登出。
     */
    LOGOUT,
    /**
     * 注册。
     */
    REGISTER,
    /**
     * 改密。
     */
    PASSWORD,
    /**
     * 业务操作。
     */
    OPERATION,
    /**
     * 安全事件。
     */
    SECURITY,
    /**
     * 审计事件。
     */
    AUDIT
}
