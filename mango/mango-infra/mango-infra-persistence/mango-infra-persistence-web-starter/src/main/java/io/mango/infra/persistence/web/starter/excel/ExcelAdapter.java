package io.mango.infra.persistence.web.starter.excel;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Excel 读写适配器。具体实现由 mango-infra-excel-starter 提供。
 */
public interface ExcelAdapter {

    /**
     * 按导入 ExcelRow 读取 Excel 行。
     */
    <ROW> List<ROW> read(MultipartFile file, ExcelImportContext context, Class<ROW> rowType);

    /**
     * 读取 Excel 并保留可归属到行和字段的转换错误。
     * <p>
     * 既有 Adapter 无需修改；默认实现将旧 read 结果包装为成功结果。
     */
    default <ROW> ExcelReadResult<ROW> readResult(MultipartFile file, ExcelImportContext context,
                                                   Class<ROW> rowType) {
        return ExcelReadResult.success(read(file, context, rowType));
    }

    /**
     * 使用导出模板写出 Excel 行。
     */
    <ROW> void write(HttpServletResponse response, ExcelExportContext context, Class<ROW> rowType, List<ROW> rows);

    /**
     * 按导入 ExcelRow 写出空导入模板。
     */
    default <ROW> void writeImportTemplate(HttpServletResponse response, ExcelImportContext context,
                                           Class<ROW> rowType) {
        write(response, new ExcelExportContext("import-template.xlsx", "", "", "sheet1",
                List.of(), List.of(), ExcelHeadGenerator.class), rowType, List.of());
    }

    /**
     * 从原始工作簿生成失败工作簿。
     */
    default byte[] createFailureWorkbook(MultipartFile file, ExcelImportContext context, List<ImportError> errors) {
        throw new IllegalStateException("当前 ExcelAdapter 不支持失败工作簿生成");
    }
}
