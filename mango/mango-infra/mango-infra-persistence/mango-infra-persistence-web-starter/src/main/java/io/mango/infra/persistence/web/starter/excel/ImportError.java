package io.mango.infra.persistence.web.starter.excel;

import java.util.Objects;

/**
 * 导入校验错误。
 */
public final class ImportError {

    private final int line;
    private final String field;
    private final String message;
    private final String code;
    private final String title;
    private final String rawValue;

    /**
     * 兼容既有错误构造方式。
     */
    public ImportError(int line, String field, String message) {
        this(line, field, message, "IMPORT_ERROR", null, null);
    }

    public ImportError(int line, String field, String message, String code, String title, String rawValue) {
        this.line = line;
        this.field = field;
        this.message = Objects.requireNonNull(message, "message");
        this.code = code == null || code.isBlank() ? "IMPORT_ERROR" : code;
        this.title = title;
        this.rawValue = rawValue;
    }

    public int line() {
        return line;
    }

    public String field() {
        return field;
    }

    public String message() {
        return message;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String rawValue() {
        return rawValue;
    }

    public int getLine() {
        return line;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getRawValue() {
        return rawValue;
    }

    public static ImportError of(int line, String field, String message) {
        return new ImportError(line, field, message);
    }

    public static ImportError cell(int line, String field, String title, String rawValue,
                                   String code, String message) {
        return new ImportError(line, field, message, code, title, rawValue);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImportError that)) {
            return false;
        }
        return line == that.line && Objects.equals(field, that.field) && Objects.equals(message, that.message)
                && Objects.equals(code, that.code) && Objects.equals(title, that.title)
                && Objects.equals(rawValue, that.rawValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, field, message, code, title, rawValue);
    }

    @Override
    public String toString() {
        return "ImportError[line=" + line + ", field=" + field + ", message=" + message + ", code=" + code
                + ", title=" + title + ", rawValue=" + rawValue + "]";
    }
}
