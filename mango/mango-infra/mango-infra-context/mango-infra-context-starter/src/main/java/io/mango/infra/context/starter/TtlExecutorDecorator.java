package io.mango.infra.context.starter;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Decorator that wraps raw Executors with Alibaba TTL (TransmittableThreadLocal) support.
 * <p>
 * Wraps {@link ScheduledExecutorService} and {@link ExecutorService} so that
 * ThreadLocal context (tenantId, traceId, etc.) propagates across thread pool boundaries.
 * <p>
 * Usage: Inject this decorator and call {@code decorate(executor)} instead of
 * using raw {@link java.util.concurrent.Executors} factory methods.
 *
 * <pre>{@code
 * // Before (loses context in async threads):
 * private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
 *
 * // After (context propagates):
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
@Component
public class TtlExecutorDecorator {

    /**
     * Wrap a ScheduledExecutorService with TTL context propagation.
     *
     * @param executor the raw executor to wrap
     * @return TTL-wrapped executor
     */
    public ScheduledExecutorService decorate(ScheduledExecutorService executor) {
        return TtlExecutors.getTtlScheduledExecutorService(executor);
    }

    /**
     * Wrap an ExecutorService with TTL context propagation.
     *
     * @param executor the raw executor to wrap
     * @return TTL-wrapped executor
     */
    public ExecutorService decorate(ExecutorService executor) {
        return TtlExecutors.getTtlExecutorService(executor);
    }

}
