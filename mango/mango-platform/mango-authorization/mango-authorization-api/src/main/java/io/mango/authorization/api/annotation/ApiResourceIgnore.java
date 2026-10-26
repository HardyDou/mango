package io.mango.authorization.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记不参与 API Resource Controller 扫描的类型或方法。
 *
 * <p>该注解只影响 Resource Provider 发现结果，不影响接口映射、访问控制或固定资源声明。</p>
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiResourceIgnore {
}
