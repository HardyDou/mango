package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 导入上下文。
 */
public record ExcelImportContext(String fileName, int headRowNumber, boolean ignoreEmptyRow, ExcelImportMode mode,
                                 String sheetName, int sheetIndex, UnknownColumnPolicy unknownColumnPolicy,
                                 String templateLocation, FailureRowPolicy failureRowPolicy) {

    public ExcelImportContext(String fileName, int headRowNumber, boolean ignoreEmptyRow, ExcelImportMode mode) {
        this(fileName, headRowNumber, ignoreEmptyRow, mode, "", 0, UnknownColumnPolicy.IGNORE, "",
                FailureRowPolicy.FAILED_ONLY);
    }

    public static ExcelImportContext of(ExcelImport annotation) {
        if (annotation == null) {
            return defaults();
        }
        String fileName = annotation.fileName() == null || annotation.fileName().isBlank()
                ? "file"
                : annotation.fileName().trim();
        return new ExcelImportContext(fileName, Math.max(annotation.headRowNumber(), 1), annotation.ignoreEmptyRow(),
                annotation.mode(), normalize(annotation.sheetName()), Math.max(annotation.sheetIndex(), 0),
                annotation.unknownColumnPolicy(), normalize(annotation.templateLocation()), annotation.failureRowPolicy());
    }

    public static ExcelImportContext of(RequestExcel annotation) {
        if (annotation == null) {
            return defaults();
        }
        String fileName = annotation.fileName() == null || annotation.fileName().isBlank()
                ? "file"
                : annotation.fileName().trim();
        return new ExcelImportContext(fileName, Math.max(annotation.headRowNumber(), 1), annotation.ignoreEmptyRow(),
                annotation.mode(), normalize(annotation.sheetName()), Math.max(annotation.sheetIndex(), 0),
                annotation.unknownColumnPolicy(), normalize(annotation.templateLocation()), annotation.failureRowPolicy());
    }

    public ExcelImportContext withMode(ExcelImportMode mode) {
        return new ExcelImportContext(fileName, headRowNumber, ignoreEmptyRow, mode == null ? this.mode : mode,
                sheetName, sheetIndex, unknownColumnPolicy, templateLocation, failureRowPolicy);
    }

    private static ExcelImportContext defaults() {
        return new ExcelImportContext("file", 1, true, ExcelImportMode.PARTIAL_SUCCESS);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
