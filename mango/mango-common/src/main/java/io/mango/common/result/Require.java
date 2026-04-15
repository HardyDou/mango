package io.mango.common.result;

import io.mango.common.exception.BizException;

/**
 * 断言工具。
 *
 * @author Mango
 */
public final class Require {

    private static final BizCode DEFAULT_BIZ_CODE = CommonCode.BAD_REQUEST;

    private Require() {
    }

    // ==================== Object 断言 ====================

    /**
     * 断言对象不为 null。
     *
     * @param obj 待校验对象。
     * @param message 失败消息。
     */
    public static void notNull(Object obj, String message) {
        notNull(obj, DEFAULT_BIZ_CODE.getCode(), message);
    }

    /**
     * 断言对象不为 null。
     *
     * @param obj 待校验对象。
     * @param bizCode 失败错误码。
     */
    public static void notNull(Object obj, BizCode bizCode) {
        notNull(obj, bizCode.getCode(), bizCode.getMessage());
    }

    /**
     * 断言对象不为 null。
     *
     * @param obj 待校验对象。
     * @param code 失败错误码。
     * @param message 失败消息。
     */
    public static void notNull(Object obj, int code, String message) {
        failWhen(obj == null, code, message);
    }

    /**
     * 断言对象为 null。
     *
     * @param obj 待校验对象。
     * @param message 失败消息。
     */
    public static void isNull(Object obj, String message) {
        isNull(obj, DEFAULT_BIZ_CODE.getCode(), message);
    }

    /**
     * 断言对象为 null。
     *
     * @param obj 待校验对象。
     * @param bizCode 失败错误码。
     */
    public static void isNull(Object obj, BizCode bizCode) {
        isNull(obj, bizCode.getCode(), bizCode.getMessage());
    }

    /**
     * 断言对象为 null。
     *
     * @param obj 待校验对象。
     * @param code 失败错误码。
     * @param message 失败消息。
     */
    public static void isNull(Object obj, int code, String message) {
        failWhen(obj != null, code, message);
    }

    // ==================== Boolean 断言 ====================

    /**
     * 断言表达式为 true。
     *
     * @param expression 待校验表达式。
     * @param message 失败消息。
     */
    public static void isTrue(boolean expression, String message) {
        isTrue(expression, DEFAULT_BIZ_CODE.getCode(), message);
    }

    /**
     * 断言表达式为 true。
     *
     * @param expression 待校验表达式。
     * @param bizCode 失败错误码。
     */
    public static void isTrue(boolean expression, BizCode bizCode) {
        isTrue(expression, bizCode.getCode(), bizCode.getMessage());
    }

    /**
     * 断言表达式为 true。
     *
     * @param expression 待校验表达式。
     * @param code 失败错误码。
     * @param message 失败消息。
     */
    public static void isTrue(boolean expression, int code, String message) {
        failWhen(!expression, code, message);
    }

    /**
     * 断言表达式为 false。
     *
     * @param expression 待校验表达式。
     * @param message 失败消息。
     */
    public static void isFalse(boolean expression, String message) {
        isFalse(expression, DEFAULT_BIZ_CODE.getCode(), message);
    }

    /**
     * 断言表达式为 false。
     *
     * @param expression 待校验表达式。
     * @param bizCode 失败错误码。
     */
    public static void isFalse(boolean expression, BizCode bizCode) {
        isFalse(expression, bizCode.getCode(), bizCode.getMessage());
    }

    /**
     * 断言表达式为 false。
     *
     * @param expression 待校验表达式。
     * @param code 失败错误码。
     * @param message 失败消息。
     */
    public static void isFalse(boolean expression, int code, String message) {
        failWhen(expression, code, message);
    }

    // ==================== String 断言 ====================

    /**
     * 断言字符串非空。
     *
     * @param str 待校验字符串。
     * @param message 失败消息。
     */
    public static void notEmpty(String str, String message) {
        failWhen(str == null || str.isEmpty(), DEFAULT_BIZ_CODE, message);
    }

    /**
     * 断言字符串非空白。
     *
     * @param str 待校验字符串。
     * @param message 失败消息。
     */
    public static void notBlank(String str, String message) {
        notBlank(str, DEFAULT_BIZ_CODE.getCode(), message);
    }

    /**
     * 断言字符串非空白。
     *
     * @param str 待校验字符串。
     * @param bizCode 失败错误码。
     */
    public static void notBlank(String str, BizCode bizCode) {
        notBlank(str, bizCode.getCode(), bizCode.getMessage());
    }

    /**
     * 断言字符串非空白。
     *
     * @param str 待校验字符串。
     * @param code 失败错误码。
     * @param message 失败消息。
     */
    public static void notBlank(String str, int code, String message) {
        failWhen(str == null || str.isBlank(), code, message);
    }

    // ==================== Collection 断言 ====================

    /**
     * 断言集合非空。
     *
     * @param collection 待校验集合。
     * @param message 失败消息。
     */
    public static void notEmpty(java.util.Collection<?> collection, String message) {
        notEmpty(collection, DEFAULT_BIZ_CODE.getCode(), message);
    }

    /**
     * 断言集合非空。
     *
     * @param collection 待校验集合。
     * @param bizCode 失败错误码。
     */
    public static void notEmpty(java.util.Collection<?> collection, BizCode bizCode) {
        notEmpty(collection, bizCode.getCode(), bizCode.getMessage());
    }

    /**
     * 断言集合非空。
     *
     * @param collection 待校验集合。
     * @param code 失败错误码。
     * @param message 失败消息。
     */
    public static void notEmpty(java.util.Collection<?> collection, int code, String message) {
        failWhen(collection == null || collection.isEmpty(), code, message);
    }

    // ==================== Number 断言 ====================

    /**
     * 断言数值大于 0。
     *
     * @param number 待校验值。
     * @param message 失败消息。
     */
    public static void positive(long number, String message) {
        failWhen(number <= 0, DEFAULT_BIZ_CODE, message);
    }

    /**
     * 断言数值大于等于 0。
     *
     * @param number 待校验值。
     * @param message 失败消息。
     */
    public static void nonNegative(long number, String message) {
        failWhen(number < 0, DEFAULT_BIZ_CODE, message);
    }

    /**
     * 断言数值在闭区间内。
     *
     * @param value 待校验值。
     * @param min 最小值。
     * @param max 最大值。
     * @param message 失败消息。
     */
    public static void inRange(long value, long min, long max, String message) {
        failWhen(value < min || value > max, DEFAULT_BIZ_CODE, message);
    }

    private static void failWhen(boolean invalid, BizCode bizCode, String message) {
        failWhen(invalid, bizCode.getCode(), message);
    }

    private static void failWhen(boolean invalid, int code, String message) {
        if (invalid) {
            throw new BizException(code, message);
        }
    }
}
