package io.mango.infra.persistence.web.starter.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 导入字段映射。
 * <p>
 * {@link #title()} 与 {@link #idx()} 必须且只能配置一个。idx 从零开始计数，title 匹配失败时不会按 idx 兜底。
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    /** 主标题。 */
    String title() default "";

    /** 固定零基列序号。 */
    int idx() default -1;

    /** 是否要求工作簿包含该列。 */
    boolean required() default false;

    /** 可接受的标题别名。 */
    String[] aliases() default {};

    /** 字典类型编码。 */
    String dictType() default "";

    /** 字段自定义转换器；配置后优先于 dictType。 */
    Class<? extends ExcelColumnConverter<?>> converter() default ExcelColumnConverter.None.class;
}
