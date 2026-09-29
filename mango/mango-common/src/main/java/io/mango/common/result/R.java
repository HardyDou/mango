package io.mango.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果。
 *
 * @param <T> 数据类型。
 * @author Mango
 */
@Data
@SuppressWarnings({"PMD.ShortClassName", "PMD.ClassNamingShouldBeCamelRule"})
public final class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码。 */
    private int code;

    /** 是否成功。 */
    private boolean success;

    /** 消息。 */
    private String msg;

    /** 数据。 */
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return ok(data, CommonCode.SUCCESS.getMessage());
    }

    public static <T> R<T> ok(T data, String msg) {
        R<T> r = new R<>();
        r.setCode(CommonCode.SUCCESS.getCode());
        r.setSuccess(true);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setSuccess(false);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(CommonCode.SERVER_ERROR.getCode(), msg);
    }

    public static <T> R<T> fail(BizCode bizCode) {
        return fail(bizCode.getCode(), bizCode.getMessage());
    }

    public static <T> R<T> fail(int code, String msg, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setSuccess(false);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    /**
     * 解包成功响应中的数据，失败响应统一转换为业务异常。
     *
     * @param bizCode 调用方业务错误码。
     * @param fallbackMessage 消息为空或数据为空时使用的兜底消息。
     * @return 非空响应数据。
     */
    public T unwrap(BizCode bizCode, String fallbackMessage) {
        String message = getMsg();
        if (message == null || message.isBlank()) {
            message = fallbackMessage;
        }
        Require.isTrue(isSuccess(), bizCode, message);
        return Require.nonNull(getData(), bizCode, fallbackMessage);
    }

    /**
     * 返回请求是否成功。
     *
     * @return 请求是否成功。
     */
    public boolean isSuccess() {
        return success;
    }
}
