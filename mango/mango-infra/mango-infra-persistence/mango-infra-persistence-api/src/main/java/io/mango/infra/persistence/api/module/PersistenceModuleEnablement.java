package io.mango.infra.persistence.api.module;

import java.util.Optional;

/**
 * 模块级持久化开关查询。
 * <p>
 * 由持久化实现提供，供 Resource 等上层装配判断「某模块在当前应用里是否存在」：模块被
 * {@code mango.persistence.flyway.modules.<module>.enabled=false} 显式关闭时，该模块的迁移不执行，
 * 依赖它表结构的装配（例如便携 Resource 声明）也必须一并跳过，否则会在启动期访问不存在的表。
 * <p>
 * 未配置过的模块一律视为启用（fail-open），因此没有持久化实现的进程无需关心本契约。
 */
public interface PersistenceModuleEnablement {

    /**
     * 判断模块的迁移是否启用。
     *
     * @param moduleCode 模块编码，例如 integration；为空时视为启用
     * @return true 表示该模块在当前应用中启用
     */
    boolean isEnabled(String moduleCode);

    /**
     * 模块被关闭时配置的跳过原因，用于启动日志与诊断。
     *
     * @param moduleCode 模块编码
     * @return 跳过原因；模块启用或未配置原因时为空
     */
    default Optional<String> skipReason(String moduleCode) {
        return Optional.empty();
    }
}
