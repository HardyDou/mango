package io.mango.infra.persistence.web.starter.excel;

/**
 * 导入校验错误。
 *
 * @param line    Excel 行号。
 * @param field   字段名；跨字段或整行错误可为空。
 * @param message 错误信息。
 */
public record ImportError(int line, String field, String message) {

    public static ImportError of(int line, String field, String message) {
        return new ImportError(line, field, message);
    }
}
