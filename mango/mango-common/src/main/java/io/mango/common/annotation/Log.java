package io.mango.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Mango
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 操作描述
     */
    String value();

    /**
     * 操作类型
     */
    OperateType type() default OperateType.OTHER;

    enum OperateType {
        /**
         * 查询
         */
        GET,
        /**
         * 新增
         */
        ADD,
        /**
         * 修改
         */
        EDIT,
        /**
         * 删除
         */
        DELETE,
        /**
         * 导出
         */
        EXPORT,
        /**
         * 导入
         */
        IMPORT,
        /**
         * 其他
         */
        OTHER
    }
}
