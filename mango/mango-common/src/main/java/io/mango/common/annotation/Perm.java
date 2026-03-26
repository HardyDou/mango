package io.mango.common.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 * 格式：{model}:{module}:{action}
 * 例如：system:user:add, system:user:edit
 *
 * @author Mango
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Perm {

    /**
     * 权限码
     */
    String value();

    /**
     * 权限描述
     */
    String desc() default "";
}
