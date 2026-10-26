package io.mango.common.credential;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明需要详情访问票据的入口。
 *
 * <p>方法返回类型已经标注 {@link DetailCredential} 时可以不写本注解；
 * 返回包装 VO 或需要指定额外入口时才显式声明。
 *
 * @author Mango
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DetailCredentialProtected {

    /**
     * 该入口对应的票据授权入口。
     *
     * @return 入口声明，留空表示从方法返回类型推断
     */
    String[] value() default {};
}
