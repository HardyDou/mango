package io.mango.authorization.data.grant;

/**
 * Grant 编解码失败异常。
 * <p>
 * 所有解码失败（格式、版本、密钥标识、签名、载荷结构）统一抛出本异常，
 * 由 {@link DataGrantVerifier} 归并为「无效」状态，不向调用方暴露具体失败细节。
 */
public class DataGrantException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造 Grant 异常。
     *
     * @param message 面向日志的失败原因。
     */
    public DataGrantException(String message) {
        super(message);
    }

    /**
     * 构造 Grant 异常。
     *
     * @param message 面向日志的失败原因。
     * @param cause   底层异常。
     */
    public DataGrantException(String message, Throwable cause) {
        super(message, cause);
    }
}
