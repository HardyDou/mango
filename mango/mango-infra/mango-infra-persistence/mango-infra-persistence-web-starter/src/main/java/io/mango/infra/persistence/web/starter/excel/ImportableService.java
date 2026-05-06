package io.mango.infra.persistence.web.starter.excel;

import java.util.List;

/**
 * 可导入服务能力。
 *
 * @param <ROW> 导入 ExcelRow 类型。
 */
public interface ImportableService<ROW> {

    /**
     * 导入 ExcelRow 类型。Excel 列由该类型的字段和注解决定。
     */
    Class<ROW> importRowType();

    /**
     * 导入前业务校验。
     * <p>
     * Bean Validation 适合校验单行字段格式；业务校验适合校验上传文件内重复、
     * 与数据库已有数据重复、跨字段组合规则等。
     */
    default List<ImportError> validateImportRows(List<ROW> rows, ExcelImportContext context) {
        return List.of();
    }

    /**
     * 导入行数据。
     */
    ImportResult importRows(List<ROW> rows);
}
