package io.mango.authorization.api.annotation;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明需要权限码的 HTTP API。
 */
@Documented
@ApiAccess(mode = ApiResourceAccessMode.PERMISSION)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionAccess {

    /**
     * 权限码。
     */
    @AliasFor(annotation = ApiAccess.class, attribute = "permission")
    String value() default "";

    /**
     * 资源描述。
     */
    @AliasFor(annotation = ApiAccess.class, attribute = "desc")
    String desc() default "";
}
