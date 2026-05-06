package io.mango.infra.persistence.api.context;

/**
 * 持久化上下文提供者。
 * <p>
 * 审计字段、租户字段和后续数据权限能力通过该接口读取当前执行上下文。
 */
public interface PersistenceContextProvider {

    /**
     * 获取当前持久化上下文。
     *
     * @return 当前上下文；无上下文时返回空上下文。
     */
    PersistenceContext currentContext();
}
