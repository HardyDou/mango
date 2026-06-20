package io.mango.infra.context.support;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 执行器装饰器，用于为原始执行器增加 Alibaba TTL 上下文传播能力。
 * <p>
 * 包装 {@link ScheduledExecutorService} 与 {@link ExecutorService}，让 MangoContext 等 ThreadLocal
 * 上下文可以跨线程池边界传播。
 * <p>
 * 使用方式：注入该装饰器后调用 {@code decorate(executor)}，不要直接暴露原始执行器。
 *
 * <pre>{@code
 * // 错误：异步线程中会丢失上下文。
 * private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
 *
 * // 正确：上下文可以传播。
 * @Autowired
 * private TtlExecutorDecorator ttlExecutorDecorator;
 * private ScheduledExecutorService cleanupScheduler;
 *
 * @PostConstruct
 * public void init() {
 *     cleanupScheduler = ttlExecutorDecorator.decorate(
 *         Executors.newSingleThreadScheduledExecutor()
 *     );
 * }
 * }</pre>
 *
 * @author Mango
 */
public class TtlExecutorDecorator {

    /**
     * 包装 Executor，使其支持 TTL 上下文传播。
     *
     * @param executor 原始执行器
     * @return 已包装的执行器
     */
    public Executor decorate(Executor executor) {
        return TtlExecutors.getTtlExecutor(executor);
    }

    /**
     * 包装 ScheduledExecutorService，使其支持 TTL 上下文传播。
     *
     * @param executor 原始执行器
     * @return 已包装的执行器
     */
    public ScheduledExecutorService decorate(ScheduledExecutorService executor) {
        return TtlExecutors.getTtlScheduledExecutorService(executor);
    }

    /**
     * 包装 ExecutorService，使其支持 TTL 上下文传播。
     *
     * @param executor 原始执行器
     * @return 已包装的执行器
     */
    public ExecutorService decorate(ExecutorService executor) {
        return TtlExecutors.getTtlExecutorService(executor);
    }

    /**
     * 包装 Runnable，使其携带当前线程上下文。
     *
     * @param runnable 原始任务
     * @return 已包装任务
     */
    public Runnable decorate(Runnable runnable) {
        return TtlRunnable.get(runnable, false, true);
    }

    /**
     * 包装 Callable，使其携带当前线程上下文。
     *
     * @param callable 原始任务
     * @return 已包装任务
     */
    public <T> Callable<T> decorate(Callable<T> callable) {
        return TtlCallable.get(callable, false, true);
    }
}
