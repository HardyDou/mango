package io.mango.infra.persistence.web.starter.excel;

import java.util.Arrays;
import java.util.List;

/**
 * Excel 导出上下文。
 */
public record ExcelExportContext(String fileName, String templateKey, String templateLocation, String sheetName,
                                 List<String> include, List<String> exclude,
                                 Class<? extends ExcelHeadGenerator> headGenerator) {

    private static final String DEFAULT_FILE_NAME = "export.xlsx";
    private static final String DEFAULT_SHEET_NAME = "sheet1";

    public static ExcelExportContext of(ExcelExport annotation, String fallbackFileName) {
        if (annotation == null) {
            return new ExcelExportContext(normalize(fallbackFileName, DEFAULT_FILE_NAME), "", "", DEFAULT_SHEET_NAME,
                    List.of(), List.of(), ExcelHeadGenerator.class);
        }
        String fileName = normalize(annotation.fileName(), normalize(fallbackFileName, DEFAULT_FILE_NAME));
        return new ExcelExportContext(fileName, normalize(annotation.templateKey(), ""),
                normalize(annotation.templateLocation(), ""), normalize(annotation.sheetName(), DEFAULT_SHEET_NAME),
                List.copyOf(Arrays.asList(annotation.include())),
                List.copyOf(Arrays.asList(annotation.exclude())),
                annotation.headGenerator());
    }

    private static String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public boolean hasCustomHeadGenerator() {
        return headGenerator != null && !headGenerator.isInterface();
    }
}
