package io.mango.resource.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

/**
 * 资源同步模式。
 *
 * <p>{@link #SYNC_ALWAYS} 表示以声明为准：创建缺失资源、覆盖声明变更、声明缺失时停用目标。
 * {@link #SYNC_ONCE} 表示只保证资源存在：不存在时创建，之后不覆盖、也不因声明消失而停用。
 *
 * <p>历史值 {@code AUTO}、{@code INIT_ONLY}、{@code MANUAL}、{@code LOCKED} 仅在读取时兼容，
 * 统一映射为上述两个值并在使用旧值时输出告警；新写入只产生 {@code SYNC_ALWAYS} 或 {@code SYNC_ONCE}。
 */
public enum ResourceSyncMode {
    SYNC_ALWAYS,
    SYNC_ONCE;

    private static final Logger log = LoggerFactory.getLogger(ResourceSyncMode.class);

    private static final Map<String, ResourceSyncMode> LEGACY_VALUES = Map.of(
            "AUTO", SYNC_ALWAYS,
            "INIT_ONLY", SYNC_ONCE,
            "MANUAL", SYNC_ONCE,
            "LOCKED", SYNC_ONCE);

    @JsonCreator
    public static ResourceSyncMode from(String value) {
        if (value == null || value.isBlank()) {
            return SYNC_ALWAYS;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        for (ResourceSyncMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        ResourceSyncMode mapped = LEGACY_VALUES.get(normalized);
        if (mapped != null) {
            log.warn("Mango resource sync mode is deprecated: value={}, migrated={}", normalized, mapped);
            return mapped;
        }
        throw new IllegalArgumentException("Unsupported resource sync mode: " + value);
    }

    @JsonValue
    public String value() {
        return name();
    }
}
