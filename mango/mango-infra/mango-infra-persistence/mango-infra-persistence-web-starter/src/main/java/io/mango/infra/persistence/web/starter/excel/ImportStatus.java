package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 导入结果状态。
 */
public enum ImportStatus {

    /** 全部成功。 */
    SUCCESS,

    /** 部分行成功。 */
    PARTIAL_SUCCESS,

    /** 没有行成功或批次失败。 */
    FAILED
}
