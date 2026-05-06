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
}
