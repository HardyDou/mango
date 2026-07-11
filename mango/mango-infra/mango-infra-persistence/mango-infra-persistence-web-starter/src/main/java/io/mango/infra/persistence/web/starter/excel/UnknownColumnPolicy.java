package io.mango.infra.persistence.web.starter.excel;

/**
 * 未声明 Excel 列的处理策略。
 */
public enum UnknownColumnPolicy {

    /** 忽略未声明列。 */
    IGNORE,

    /** 将未声明列作为结构错误。 */
    ERROR
}
