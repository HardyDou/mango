package io.mango.infra.persistence.web.starter.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 导入参数解析注解。
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestExcel {

    /**
     * 上传文件字段名。
     */
    String fileName() default "file";

    /**
     * 表头行数。
     */
    int headRowNumber() default 1;

    /**
     * 是否忽略空行。
     */
    boolean ignoreEmptyRow() default true;

    /**
     * 导入失败处理模式。
     */
    ExcelImportMode mode() default ExcelImportMode.PARTIAL_SUCCESS;
}
