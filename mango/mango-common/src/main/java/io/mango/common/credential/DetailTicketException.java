package io.mango.common.credential;

/**
 * 详情访问票据校验失败异常。
 *
 * <p>失败原因只用于服务端诊断与审计，响应体不区分「资源不存在」与「无权限」。
 *
 * @author Mango
 */
public class DetailTicketException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    /**
     * 构造异常。
     *
     * @param reason 失败原因
     */
    public DetailTicketException(Reason reason) {
        super("详情访问票据校验失败: " + reason.name());
        this.reason = reason;
    }

    /**
     * 返回失败原因。
     *
     * @return 失败原因
     */
    public Reason reason() {
        return reason;
    }

    /**
     * 失败原因。
     */
    public enum Reason {

        /** 请求未携带票据。 */
        MISSING,

        /** 票据格式或签名非法。 */
        INVALID,

        /** 票据已过期。 */
        EXPIRED,

        /** 票据不属于当前主体。 */
        SCOPE_DENIED,

        /** 票据未授权当前入口。 */
        WRONG_ENDPOINT,

        /** 票据未授权当前数据。 */
        DATA_NOT_ALLOWED
    }
}
