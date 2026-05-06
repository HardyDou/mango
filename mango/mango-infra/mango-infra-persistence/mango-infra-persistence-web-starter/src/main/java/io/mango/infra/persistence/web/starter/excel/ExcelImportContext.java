package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 导入上下文。
 */
public record ExcelImportContext(String fileName, int headRowNumber, boolean ignoreEmptyRow, ExcelImportMode mode) {

    public static ExcelImportContext of(ExcelImport annotation) {
        if (annotation == null) {
            return new ExcelImportContext("file", 1, true, ExcelImportMode.PARTIAL_SUCCESS);
        }
        String fileName = annotation.fileName() == null || annotation.fileName().isBlank()
                ? "file"
                : annotation.fileName().trim();
        return new ExcelImportContext(fileName, Math.max(annotation.headRowNumber(), 1), annotation.ignoreEmptyRow(),
                annotation.mode());
    }

    public static ExcelImportContext of(RequestExcel annotation) {
        if (annotation == null) {
            return new ExcelImportContext("file", 1, true, ExcelImportMode.PARTIAL_SUCCESS);
        }
        String fileName = annotation.fileName() == null || annotation.fileName().isBlank()
                ? "file"
                : annotation.fileName().trim();
        return new ExcelImportContext(fileName, Math.max(annotation.headRowNumber(), 1), annotation.ignoreEmptyRow(),
                annotation.mode());
    }

    public ExcelImportContext withMode(ExcelImportMode mode) {
        return new ExcelImportContext(fileName, headRowNumber, ignoreEmptyRow, mode == null ? this.mode : mode);
    }
}
