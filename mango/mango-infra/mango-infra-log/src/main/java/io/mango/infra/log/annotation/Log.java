package io.mango.infra.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * Moved from mango-common to mango-infra-log.
 *
 * @author Mango
 */
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
