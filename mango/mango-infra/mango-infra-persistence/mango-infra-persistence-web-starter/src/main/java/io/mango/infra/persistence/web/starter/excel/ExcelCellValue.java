package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 单元格读取值。
 *
 * @param rawText 用户在 Excel 中看到的格式化文本
 * @param formula 公式文本；非公式单元格为空
 * @param value 公式计算结果或底层单元格值
 * @param cellType 单元格类型名称
 * @param line 一开始计数的 Excel 行号
 * @param columnIndex 零开始计数的 Excel 列号
 */
public record ExcelCellValue(String rawText, String formula, Object value, String cellType,
                             int line, int columnIndex) {
}
