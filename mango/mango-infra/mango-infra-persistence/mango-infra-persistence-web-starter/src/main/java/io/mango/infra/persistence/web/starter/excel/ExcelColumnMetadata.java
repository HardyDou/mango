package io.mango.infra.persistence.web.starter.excel;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Excel 字段映射元数据。
 *
 * @param field Java 字段
 * @param title 主标题；idx 模式下可为空
 * @param aliases 标题别名
 * @param dictType 字典类型；未配置时为空
 * @param targetType 字段目标类型
 * @param columnIndex 实际零基列号
 */
public record ExcelColumnMetadata(Field field, String title, List<String> aliases, String dictType,
                                  Class<?> targetType, int columnIndex) {
}
