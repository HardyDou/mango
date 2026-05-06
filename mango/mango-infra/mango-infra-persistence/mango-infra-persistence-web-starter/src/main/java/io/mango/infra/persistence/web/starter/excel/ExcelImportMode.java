package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 导入失败处理模式。
 */
public enum ExcelImportMode {

    /**
     * 校验失败行跳过，合法行继续导入。
     */
    PARTIAL_SUCCESS,

    /**
     * 只要存在失败行，整批不导入。
     */
    ALL_SUCCESS
}
