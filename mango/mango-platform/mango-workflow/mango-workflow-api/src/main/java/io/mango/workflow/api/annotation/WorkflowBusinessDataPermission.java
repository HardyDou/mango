package io.mango.workflow.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明业务模块为指定 businessType 提供 Workflow 数据权限校验。
 * <p>
 * 注解用于让业务 Provider 的归属和审计清晰可见；运行时校验由
 * {@code WorkflowBusinessApplyDataPermissionProvider} 执行。
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WorkflowBusinessDataPermission {

    /**
     * Provider 负责的业务类型。
     */
    String businessType();
}
