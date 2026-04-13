package io.mango.common.annotation;

/**
 * 日志类型
 *
 * @author Mango
 */
public enum LogType {
    /**
     * 登录
     */
    LOGIN,
    /**
     * 登出
     */
    LOGOUT,
    /**
     * 注册
     */
    REGISTER,
    /**
     * 改密
     */
    PASSWORD,
    /**
     * 业务操作
     */
    OPERATION,
    /**
     * 安全（权限校验失败等）
     */
    SECURITY,
    /**
     * 审计
     */
    AUDIT
}
