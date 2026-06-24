package io.mango.system.api.enums;

/**
 * 系统配置选项来源。
 */
public enum ConfigOptionSourceEnum {
    /**
     * 使用系统配置自身维护的 JSON 选项。
     */
    CUSTOM,
    /**
     * 使用系统字典数据作为选项。
     */
    DICT
}
