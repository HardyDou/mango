package io.mango.common.result;

/**
 * 业务码接口。
 * 每个模块定义自己的错误码枚举并实现此接口。
 *
 * @author Mango
 */
public interface BizCode {

    /**
     * 返回业务状态码。
     *
     * @return 业务状态码。
     */
    int getCode();

    /**
     * 返回默认消息。
     *
     * @return 默认消息。
     */
    String getMessage();
}
