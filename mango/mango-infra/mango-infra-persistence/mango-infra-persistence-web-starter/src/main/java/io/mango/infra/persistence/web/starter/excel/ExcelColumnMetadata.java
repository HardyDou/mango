package io.mango.infra.persistence.web.starter.excel;

import java.util.List;

/**
 * Excel 字段映射元数据。
 *
 * @param fieldName Java 字段名称
 * @param title 主标题；idx 模式下可为空
 * @param aliases 标题别名
 * @param dictType 字典类型；未配置时为空
 * @param targetType 字段目标类型
 * @param columnIndex 实际零基列号
 */
public record ExcelColumnMetadata(String fieldName, String title, List<String> aliases, String dictType,
                                  Class<?> targetType, int columnIndex) {

    public ExcelColumnMetadata {
        aliases = List.copyOf(aliases);
    }

    @Override
    public List<String> aliases() {
        return List.copyOf(aliases);
    }
}
