package io.mango.infra.context.starter;

/**
 * Mango 运行时上下文线程池名称常量。
 *
 * @author Mango
 */
public final class MangoContextExecutors {

    /**
     * 支持 MangoContext 传播的异步线程池。
     * <p>
     * 业务方法优先使用 {@link TtlAsync}，不要直接写线程池名称字符串。
     */
    public static final String CONTEXT = "mangoContextExecutor";

    private MangoContextExecutors() {
    }
}
