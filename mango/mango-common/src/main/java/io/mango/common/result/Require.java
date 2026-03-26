package io.mango.common.result;

import lombok.extern.slf4j.Slf4j;

/**
 * 断言工具
 * 抛出异常时只打印 warn 日志，不走 err 处理
 *
 * @author Mango
 */
@Slf4j
public final class Require {

    private Require() {
    }

    // ==================== Object 断言 ====================

    /**
     * 不能为空
     */
    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new BizException(400, message);
        }
    }

    /**
     * 不能为空
     */
    public static void notNull(Object obj, BizCode bizCode) {
        if (obj == null) {
            throw new BizException(bizCode.getCode(), bizCode.getMessage());
        }
    }

    /**
     * 不能为空
     */
    public static void notNull(Object obj, int code, String message) {
        if (obj == null) {
            throw new BizException(code, message);
        }
    }

    /**
     * 必须为空
     */
    public static void isNull(Object obj, String message) {
        if (obj != null) {
            throw new BizException(400, message);
        }
    }

    /**
     * 必须为空
     */
    public static void isNull(Object obj, BizCode bizCode) {
        if (obj != null) {
            throw new BizException(bizCode.getCode(), bizCode.getMessage());
        }
    }

    // ==================== Boolean 断言 ====================

    /**
     * 必须为 true
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BizException(400, message);
        }
    }

    /**
     * 必须为 true
     */
    public static void isTrue(boolean expression, BizCode bizCode) {
        if (!expression) {
            throw new BizException(bizCode.getCode(), bizCode.getMessage());
        }
    }

    /**
     * 必须为 true
     */
    public static void isTrue(boolean expression, int code, String message) {
        if (!expression) {
            throw new BizException(code, message);
        }
    }

    /**
     * 必须为 false
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BizException(400, message);
        }
    }

    /**
     * 必须为 false
     */
    public static void isFalse(boolean expression, BizCode bizCode) {
        if (expression) {
            throw new BizException(bizCode.getCode(), bizCode.getMessage());
        }
    }

    // ==================== String 断言 ====================

    /**
     * 不能为空字符串
     */
    public static void notEmpty(String str, String message) {
        if (str == null || str.isEmpty()) {
            throw new BizException(400, message);
        }
    }

    /**
     * 不能为空白字符串
     */
    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) {
            throw new BizException(400, message);
        }
    }

    /**
     * 不能为空白字符串
     */
    public static void notBlank(String str, BizCode bizCode) {
        if (str == null || str.isBlank()) {
            throw new BizException(bizCode.getCode(), bizCode.getMessage());
        }
    }

    // ==================== Collection 断言 ====================

    /**
     * 不能为空集合
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(400, message);
        }
    }

    /**
     * 不能为空集合
     */
    public static void notEmpty(Collection<?> collection, BizCode bizCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(bizCode.getCode(), bizCode.getMessage());
        }
    }

    // ==================== Number 断言 ====================

    /**
     * 必须大于 0
     */
    public static void positive(long number, String message) {
        if (number <= 0) {
            throw new BizException(400, message);
        }
    }

    /**
     * 必须大于等于 0
     */
    public static void nonNegative(long number, String message) {
        if (number < 0) {
            throw new BizException(400, message);
        }
    }

    /**
     * 必须在范围内
     */
    public static void inRange(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw new BizException(400, message);
        }
    }
}
