package io.mango.common.credential;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明元素的详情访问入口。
 *
 * <p>标注在列表元素 VO 上：该 VO 的每一行在列表响应中都会获得一张详情访问票据，
 * 票据只允许通过这里声明的入口读取该行的详情。声明值统一写成「HTTP 方法 空格 路径」，
 * 例如 {@code "GET /cms/open/contents/detail"}。
 *
 * @author Mango
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DetailCredential {

    /**
     * 允许访问该行详情的入口列表。
     *
     * @return 入口声明，格式为「方法 路径」
     */
    String[] value();

    /**
     * 行数据主键字段名。
     *
     * @return 字段名，默认 {@code id}
     */
    String idField() default "id";
}
