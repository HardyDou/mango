package io.mango.infra.persistence.web.starter.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 导出配置。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExport {

    /**
     * 下载文件名。
     */
    String fileName() default "export.xlsx";

    /**
     * 模板标识。可用于数据库、对象存储或模板中心查找带样式模板。
     */
    String templateKey() default "";

    /**
     * 模板位置。可用于 classpath 或文件路径模板。
     */
    String templateLocation() default "";

    /**
     * Sheet 名称。
     */
    String sheetName() default "sheet1";

    /**
     * 仅导出的字段名。
     */
    String[] include() default {};

    /**
     * 不导出的字段名。
     */
    String[] exclude() default {};

    /**
     * 自定义表头生成器。未指定时由 ExportExcelRow 字段和 Excel starter 注解决定表头。
     */
    Class<? extends ExcelHeadGenerator> headGenerator() default ExcelHeadGenerator.class;
}
