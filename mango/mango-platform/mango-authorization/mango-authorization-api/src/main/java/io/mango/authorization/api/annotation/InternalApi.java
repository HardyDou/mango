package io.mango.authorization.api.annotation;

import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明仅允许内部可信调用的 HTTP API。
 */
@Documented
@ApiAccess(mode = ApiResourceAccessMode.INTERNAL)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalApi {

    /**
     * 资源描述。
     */
    @AliasFor(annotation = ApiAccess.class, attribute = "desc")
    String desc() default "";
}
