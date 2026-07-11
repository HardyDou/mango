package io.mango.infra.persistence.web.starter.excel;

/**
 * Excel 失败工作簿存储扩展点。
 */
@FunctionalInterface
public interface ExcelFailureFileStore {

    /**
     * 保存当前租户的失败工作簿。
     *
     * @param fileName 下载文件名
     * @param contentType 内容类型
     * @param content 工作簿字节
     * @param context 当前导入上下文
     * @return Mango File 文件 ID
     */
    Long store(String fileName, String contentType, byte[] content, ExcelImportContext context);
}
