package io.mango.authorization.api.annotation;

import io.mango.authorization.api.enums.ApiResourceAccessMode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 HTTP API 资源访问策略。
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAccess {

    /**
     * 接口资源同步时使用的访问模式。
     */
    ApiResourceAccessMode mode() default ApiResourceAccessMode.LOGIN;

    /**
     * {@link #mode()} 为 {@link ApiResourceAccessMode#PERMISSION} 时必填的权限码。
     */
    String permission() default "";

    /**
     * 资源描述。
     */
    String desc() default "";
}
