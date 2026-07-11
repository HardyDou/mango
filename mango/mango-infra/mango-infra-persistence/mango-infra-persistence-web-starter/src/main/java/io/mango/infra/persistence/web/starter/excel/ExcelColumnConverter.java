package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 字段自定义转换器。
 *
 * @param <T> 目标字段类型
 */
@FunctionalInterface
public interface ExcelColumnConverter<T> {

    /**
     * 将单元格值转换为字段值。
     *
     * @param value 单元格原始值和位置信息
     * @param metadata 字段映射元数据
     * @param context 当前导入上下文
     * @return 目标字段值
     */
    T convert(ExcelCellValue value, ExcelColumnMetadata metadata, ExcelImportContext context);

    /**
     * 未配置自定义转换器时使用的标记类型。
     */
    final class None implements ExcelColumnConverter<Object> {

        @Override
        public Object convert(ExcelCellValue value, ExcelColumnMetadata metadata, ExcelImportContext context) {
            throw new IllegalStateException("ExcelColumnConverter.None 不能直接执行");
        }
    }
}
