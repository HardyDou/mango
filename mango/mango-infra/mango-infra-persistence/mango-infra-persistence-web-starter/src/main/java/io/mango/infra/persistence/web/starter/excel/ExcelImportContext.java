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
        String fileName = normalizeFileName(annotation.fileName());
        return new ExcelImportContext(fileName, Math.max(annotation.headRowNumber(), 1), annotation.ignoreEmptyRow(),
                annotation.mode(), normalize(annotation.sheetName()), Math.max(annotation.sheetIndex(), 0),
                annotation.unknownColumnPolicy(), normalize(annotation.templateLocation()), annotation.failureRowPolicy());
    }

    public static ExcelImportContext of(RequestExcel annotation) {
        if (annotation == null) {
            return defaults();
        }
        String fileName = normalizeFileName(annotation.fileName());
        return new ExcelImportContext(fileName, Math.max(annotation.headRowNumber(), 1), annotation.ignoreEmptyRow(),
                annotation.mode(), normalize(annotation.sheetName()), Math.max(annotation.sheetIndex(), 0),
                annotation.unknownColumnPolicy(), normalize(annotation.templateLocation()), annotation.failureRowPolicy());
    }

    public ExcelImportContext withMode(ExcelImportMode mode) {
        ExcelImportMode selectedMode = mode;
        if (selectedMode == null) {
            selectedMode = this.mode;
        }
        return new ExcelImportContext(fileName, headRowNumber, ignoreEmptyRow, selectedMode,
                sheetName, sheetIndex, unknownColumnPolicy, templateLocation, failureRowPolicy);
    }

    private static ExcelImportContext defaults() {
        return new ExcelImportContext("file", 1, true, ExcelImportMode.PARTIAL_SUCCESS);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.trim();
    }
}
