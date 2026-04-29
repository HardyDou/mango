package io.mango.infra.context.starter;

import com.alibaba.ttl.TtlRunnable;
import org.springframework.core.task.TaskDecorator;

/**
 * Mango 运行时上下文任务装饰器。
 * <p>
 * Spring 线程池提交任务时会调用该装饰器，装饰后的任务可以携带提交线程中的 MangoContext。
 *
 * @author Mango
 */
public class MangoContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return TtlRunnable.get(runnable, false, true);
    }
}
