package io.mango.infra.persistence.web.starter.excel;

/**
 * 失败工作簿的数据行保留策略。
 */
public enum FailureRowPolicy {

    /** 只保留导入失败的数据行。 */
    FAILED_ONLY,

    /** 保留全部原始数据行，并只为失败行追加原因。 */
    ALL_ROWS
}
