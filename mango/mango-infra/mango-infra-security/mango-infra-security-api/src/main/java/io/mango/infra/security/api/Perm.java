package io.mango.infra.security.api;

import java.lang.annotation.*;

/**
 * 方法级权限控制注解。
 * <p>
 * 格式：{model}:{module}:{action}
 * 示例：system:user:add、system:user:edit
 *
 * @author Mango
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Perm {

    /**
     * 权限码。
     */
    String value();

    /**
     * 权限描述。
     */
    String desc() default "";
}
