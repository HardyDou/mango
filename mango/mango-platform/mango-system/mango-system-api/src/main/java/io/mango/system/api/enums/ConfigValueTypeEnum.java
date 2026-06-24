package io.mango.system.api.enums;

/**
 * 系统配置值展示与编辑类型。
 */
public enum ConfigValueTypeEnum {
    /**
     * 开关。
     */
    BOOLEAN,
    /**
     * 文本。
     */
    STRING,
    /**
     * 数字。
     */
    NUMBER,
    /**
     * 单选按钮。
     */
    RADIO,
    /**
     * 下拉单选。
     */
    SELECT,
    /**
     * 下拉多选，配置值为 JSON 字符串数组。
     */
    MULTI_SELECT,
    /**
     * 日期，格式 yyyy-MM-dd。
     */
    DATE,
    /**
     * 日期区间，配置值为 JSON 字符串数组。
     */
    DATE_RANGE
}
