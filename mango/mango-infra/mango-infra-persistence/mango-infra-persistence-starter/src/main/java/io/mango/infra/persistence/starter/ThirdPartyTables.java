package io.mango.infra.persistence.starter;

import java.util.Locale;
import java.util.Set;

/**
 * 第三方组件表边界。
 *
 * <p>{@code act_*}、{@code flw_*} 是 Mango 直接集成的 Flowable 引擎表，列名和租户语义由引擎数据模型定义：
 * 引擎表只有 {@code TENANT_ID_} 等带下划线后缀的列，没有 Mango 业务表规范要求的 {@code tenant_id}、
 * 审计和组织字段。启动期结构校验和 MyBatis-Plus 租户拦截器共用本边界，避免同一条事实在两处各写一份。
 */
final class ThirdPartyTables {

    private static final Set<String> PREFIXES = Set.of("act_", "flw_");

    private ThirdPartyTables() {
    }

    /**
     * 判断表名是否属于第三方组件表。
     *
     * @param tableName SQL 中引用的表名，可为 schema 限定名；忽略大小写和首尾空白
     * @return 以 {@code act_} 或 {@code flw_} 开头时返回 {@code true}
     */
    static boolean isThirdParty(String tableName) {
        if (tableName == null) {
            return false;
        }
        String normalized = tableName.trim().toLowerCase(Locale.ROOT);
        return PREFIXES.stream().anyMatch(normalized::startsWith);
    }
}
