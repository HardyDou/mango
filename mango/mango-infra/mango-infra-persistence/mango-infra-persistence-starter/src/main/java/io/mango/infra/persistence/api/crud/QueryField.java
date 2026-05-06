package io.mango.infra.persistence.api.crud;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询对象字段到数据库条件的映射。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {

    /**
     * 数据库列名。为空时使用字段名的下划线形式。
     */
    String column() default "";

    /**
     * 查询类型。
     */
    QueryType type() default QueryType.EQ;
}
