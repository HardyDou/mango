package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 字典 label 到 value 的解析扩展点。
 */
@FunctionalInterface
public interface ExcelDictionaryProvider {

    /**
     * 解析当前租户字典值。
     *
     * @param dictType 字典类型编码
     * @param label Excel 中的字典展示文本
     * @param metadata 字段映射元数据
     * @param context 当前导入上下文
     * @return 字典存储值
     */
    String resolveValue(String dictType, String label, ExcelColumnMetadata metadata, ExcelImportContext context);
}
