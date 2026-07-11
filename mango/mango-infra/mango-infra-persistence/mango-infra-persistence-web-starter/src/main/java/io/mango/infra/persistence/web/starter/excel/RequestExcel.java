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
     * 数据 Sheet 名称。非空时优先于 sheetIndex。
     */
    String sheetName() default "";

    /**
     * 数据 Sheet 的零基序号。
     */
    int sheetIndex() default 0;

    /**
     * 是否忽略空行。
     */
    boolean ignoreEmptyRow() default true;

    /**
     * 未声明列处理策略。
     */
    UnknownColumnPolicy unknownColumnPolicy() default UnknownColumnPolicy.IGNORE;

    /**
     * classpath 原始模板位置。
     */
    String templateLocation() default "";

    /**
     * 失败工作簿的数据行保留策略。
     */
    FailureRowPolicy failureRowPolicy() default FailureRowPolicy.FAILED_ONLY;

    /**
     * 导入失败处理模式。
     */
    ExcelImportMode mode() default ExcelImportMode.PARTIAL_SUCCESS;
}
