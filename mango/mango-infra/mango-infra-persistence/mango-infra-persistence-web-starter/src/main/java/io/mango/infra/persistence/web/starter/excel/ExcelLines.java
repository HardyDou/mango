package io.mango.infra.persistence.web.starter.excel;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Excel 行号处理工具。
 */
public final class ExcelLines {

    private ExcelLines() {
    }

    public static void fillLineNumbers(List<?> rows, ExcelImportContext context) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            fillLineNumber(rows.get(i), lineNumber(context, i));
        }
    }

    public static int lineNumber(ExcelImportContext context, int rowIndex) {
        return context.headRowNumber() + rowIndex + 1;
    }

    private static void fillLineNumber(Object row, int line) {
        if (row == null) {
            return;
        }
        Class<?> type = row.getClass();
        while (type != null && !Object.class.equals(type)) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(ExcelLine.class)) {
                    setLineNumber(row, field, line);
                }
            }
            type = type.getSuperclass();
        }
    }

    private static void setLineNumber(Object row, Field field, int line) {
        try {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            if (Long.class.equals(fieldType) || long.class.equals(fieldType)) {
                field.set(row, (long) line);
            } else if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
                field.set(row, line);
            } else if (String.class.equals(fieldType)) {
                field.set(row, String.valueOf(line));
            }
        } catch (IllegalAccessException ignored) {
            // 行号是导入辅助信息，无法写入时不影响主流程。
        }
    }
}
