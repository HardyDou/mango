package io.mango.infra.persistence.web.starter.excel;

import java.util.List;

/**
 * Excel 表头生成器。
 */
public interface ExcelHeadGenerator {

    /**
     * 生成表头。外层列表代表列，内层列表代表多级表头。
     */
    List<List<String>> head(Class<?> rowType);
}
