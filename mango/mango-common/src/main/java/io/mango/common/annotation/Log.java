package io.mango.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Mango
 * @deprecated Use {@link io.mango.infra.log.annotation.Log} instead.
 *             This class remains here for backward compatibility.
 *             Will be removed in a future version.
 */
@Deprecated
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 操作描述
     */
    String value();

    /**
     * 日志类型
     */
    LogType type() default LogType.OPERATION;
}
