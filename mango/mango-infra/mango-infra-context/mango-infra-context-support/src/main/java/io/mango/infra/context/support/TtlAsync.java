package io.mango.infra.context.support;

import org.springframework.scheduling.annotation.Async;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用支持 TTL 上下文传播的异步线程池执行方法。
 * <p>
 * 业务代码需要跨线程读取 MangoContext 时，使用该注解替代直接编写
 * {@code @Async("mangoContextExecutor")}。
 *
 * @author Mango
 */
@Documented
@Async(MangoContextExecutors.CONTEXT)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TtlAsync {
}
