package io.mango.common.result;

/**
 * 业务码接口
 * 每个模块定义自己的 Code 枚举实现此接口
 *
 * @author Mango
 */
public interface BizCode {

    /**
     * 获取状态码
     */
    int getCode();

    /**
     * 获取消息
     */
    String getMessage();
}
