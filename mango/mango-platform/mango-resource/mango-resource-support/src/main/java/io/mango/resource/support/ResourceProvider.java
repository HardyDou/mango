package io.mango.resource.support;

import io.mango.resource.support.model.ResourceDeclaration;

import java.util.List;

/**
 * 业务模块资源声明提供者。
 */
public interface ResourceProvider {

    /**
     * Whether this provider can produce a deterministic declaration set during non-Web bootstrap.
     *
     * @return true when the provider participates in bootstrap resource publication
     */
    default boolean participatesInBootstrap() {
        return true;
    }

    /**
     * 当前 Provider 管理的来源模块编码。
     * <p>
     * 当某模块删除了全部资源声明时，同步服务仍需要知道该模块的历史注册记录范围，
     * 才能把缺失资源逻辑禁用。
     *
     * @return 模块编码列表。
     */
    default List<String> moduleCodes() {
        return List.of();
    }

    /**
     * 返回当前模块声明的资源。
     *
     * @return 资源声明列表。
     */
    List<ResourceDeclaration> provide();
}
