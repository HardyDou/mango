package io.mango.authorization.api.annotation;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 声明匿名可访问的 HTTP 资源。 */
@Documented
@ApiAccess(mode = ApiResourceAccessMode.PUBLIC)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicAccess {

    @AliasFor(annotation = ApiAccess.class, attribute = "version")
    int version() default 1;

    @AliasFor(annotation = ApiAccess.class, attribute = "desc")
    String desc() default "";
}
