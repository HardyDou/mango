package io.mango.resource.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * 资源归属。
 *
 * <ul>
 *   <li>{@link #RESOURCE_MANAGED}：由 Resource 声明创建或更新，未被人工接管。</li>
 *   <li>{@link #MANUAL_TAKEOVER}：运行时被人工接管，后续不得被声明自动覆盖或删除。</li>
 *   <li>{@link #UNKNOWN}：历史数据无法证明来源，禁止自动覆盖或删除；声明可证明归属时升级为
 *       {@link #RESOURCE_MANAGED}。</li>
 * </ul>
 */
public enum ResourceOwnership {
    RESOURCE_MANAGED,
    MANUAL_TAKEOVER,
    UNKNOWN;

    @JsonCreator
    public static ResourceOwnership from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return ResourceOwnership.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String value() {
        return name();
    }
}
