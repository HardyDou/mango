package io.mango.infra.persistence.web.starter.excel;

import java.util.List;

/**
 * Excel 读取阶段结果。
 *
 * @param rows 成功转换的行
 * @param errors 结构和单元格转换错误
 * @param <ROW> 行模型类型
 */
public record ExcelReadResult<ROW>(List<ROW> rows, List<ImportError> errors) {

    public ExcelReadResult {
        if (rows == null) {
            rows = List.of();
        } else {
            rows = List.copyOf(rows);
        }
        if (errors == null) {
            errors = List.of();
        } else {
            errors = List.copyOf(errors);
        }
    }

    public static <ROW> ExcelReadResult<ROW> success(List<ROW> rows) {
        return new ExcelReadResult<>(rows, List.of());
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
