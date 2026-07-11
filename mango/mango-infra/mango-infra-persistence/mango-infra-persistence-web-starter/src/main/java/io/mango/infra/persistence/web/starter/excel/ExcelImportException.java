package io.mango.infra.persistence.web.starter.excel;

import java.util.List;

/**
 * Excel 工作簿结构或字段转换异常。
 */
public class ExcelImportException extends RuntimeException {

    private final List<ImportError> errors;

    public ExcelImportException(String message, List<ImportError> errors) {
        super(message);
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public ExcelImportException(String message, Throwable cause) {
        super(message, cause);
        this.errors = List.of();
    }

    public List<ImportError> getErrors() {
        return errors;
    }
}
