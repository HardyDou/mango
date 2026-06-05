package io.mango.infra.persistence.starter.datasource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定当前方法或类型使用的 Mango 数据源。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PersistenceDataSource {

    /**
     * 数据源名称。
     */
    String value();
}
