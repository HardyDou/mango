package io.mango.infra.persistence.api.scope;

import java.util.Collections;
import java.util.Set;

/**
 * 数据权限范围规则。
 *
 * @param mode 范围模式。
 * @param values 范围值。
 */
public record DataScopeRule(Mode mode, Set<String> values) {

    public DataScopeRule {
        values = values == null ? Collections.emptySet() : Set.copyOf(values);
    }

    /**
     * 数据权限范围模式。
     */
    public enum Mode {
        /**
         * 全部数据。
         */
        ALL,
        /**
         * 当前主体自己的数据。
         */
        SELF,
        /**
         * 指定组织或部门范围。
         */
        ORG,
        /**
         * 指定租户范围。
         */
        TENANT,
        /**
         * 自定义范围。
         */
        CUSTOM
    }
}
