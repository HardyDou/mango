package io.mango.infra.excel.starter;

import io.mango.infra.persistence.web.starter.excel.ExcelColumn;
import io.mango.infra.persistence.web.starter.excel.ExcelColumnMetadata;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 一次 Excel 读写操作中的字段映射。
 */
final class ExcelColumnBinding {

    private final Field field;
    private final ExcelColumn annotation;
    private int columnIndex = -1;

    ExcelColumnBinding(Field field, ExcelColumn annotation) {
        this.field = field;
        this.annotation = annotation;
    }

    Field field() {
        return field;
    }

    ExcelColumn annotation() {
        return annotation;
    }

    int configuredIndex() {
        return annotation.idx();
    }

    int columnIndex() {
        return columnIndex;
    }

    void columnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    String displayTitle() {
        if (StringUtils.hasText(annotation.title())) {
            return annotation.title().trim();
        }
        return field.getName();
    }

    ExcelColumnMetadata metadata() {
        return new ExcelColumnMetadata(field.getName(), annotation.title(), List.of(annotation.aliases()),
                annotation.dictType(), field.getType(), columnIndex);
    }
}
