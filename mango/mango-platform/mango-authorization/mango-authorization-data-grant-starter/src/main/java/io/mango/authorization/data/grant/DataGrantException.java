package io.mango.authorization.data.grant;

/**
 * Grant 编解码失败异常。
 * <p>
 * 所有解码失败（格式、版本、密钥标识、签名、载荷结构）统一抛出本异常，
 * 由 {@link DataGrantVerifier} 归并为「无效」状态，不向调用方暴露具体失败细节。
 */
public class DataGrantException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    /**
     * 构建失败分类。
     *
     * <p>分类用于程序判定，文案只用于展示；调用方不应依赖 {@link #getMessage()} 做分支判断。
     */
    public enum Reason {

        /** 通用失败，未细分。 */
        INVALID,

        /** 令牌分段、编码格式不正确。 */
        FORMAT,

        /** 版本或算法不受支持。 */
        VERSION_UNSUPPORTED,

        /** 签名密钥标识不匹配。 */
        KEY_ID_MISMATCH,

        /** 签名校验失败。 */
        SIGNATURE_MISMATCH,

        /** 载荷结构或字段不合法。 */
        PAYLOAD
    }

    /**
     * 构造 Grant 异常。
     *
     * @param message 面向日志的失败原因。
     */
    public DataGrantException(String message) {
        this(Reason.INVALID, message);
    }

    /**
     * 构造 Grant 异常。
     *
     * @param reason  失败分类。
     * @param message 面向日志的失败原因。
     */
    public DataGrantException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * 构造 Grant 异常。
     *
     * @param message 面向日志的失败原因。
     * @param cause   底层异常。
     */
    public DataGrantException(String message, Throwable cause) {
        this(Reason.INVALID, message, cause);
    }

    /**
     * 构造 Grant 异常。
     *
     * @param reason  失败分类。
     * @param message 面向日志的失败原因。
     * @param cause   底层异常。
     */
    public DataGrantException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /**
     * 失败分类。
     *
     * @return 分类，永不为 {@code null}。
     */
    public Reason reason() {
        return reason;
    }
}
