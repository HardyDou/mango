package io.mango.infra.persistence.web.starter.excel;

import java.util.List;

/**
 * 可导出服务能力。
 *
 * @param <Q> 查询类型。
 * @param <ROW> 导出 ExcelRow 类型。
 */
public interface ExportableService<Q, ROW> {

    /**
     * 导出 ExcelRow 类型。Excel 列由该类型的字段和注解决定。
     */
    Class<ROW> exportRowType();

    /**
     * 导出文件名。
     */
    default String exportFileName() {
        return "export.xlsx";
    }

    /**
     * 查询导出行。
     */
    List<ROW> exportRows(Q query);
}
