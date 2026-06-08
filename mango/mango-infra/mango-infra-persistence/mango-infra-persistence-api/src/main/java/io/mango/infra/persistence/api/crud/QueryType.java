package io.mango.infra.persistence.api.crud;

/**
 * 标准查询条件类型。
 */
public enum QueryType {
    EQ,
    NE,
    LIKE,
    LEFT_LIKE,
    RIGHT_LIKE,
    IN,
    BETWEEN,
    GE,
    GT,
    LE,
    LT
}
