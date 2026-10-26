package io.mango.common.credential;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明查询或命令对象中被保护的详情数据标识。
 *
 * <p>标注在 Query/Command 的字段上，详情入口据此取出本次请求要访问的数据 ID，
 * 并与票据中绑定的 ID 比较。同一个入口只允许一个被保护字段；需要「ID 或编码」
 * 两种定位方式时，把其余字段标记为 {@link #alternative()}。
 *
 * @author Mango
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DetailCredentialId {

    /**
     * 是否为可替代定位字段。
     *
     * @return {@code true} 表示与主标识二选一，不强制存在
     */
    boolean alternative() default false;
}
