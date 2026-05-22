package io.mango.template.core.service;

/**
 * 模板文件存取适配口。
 */
public interface ITemplateFileStore {

    /**
     * 保存模板生成文件并返回文件 ID。
     *
     * @param content 文件内容。
     * @param fileName 文件名。
     * @param contentType 文件内容类型。
     * @param purpose 文件用途。
     * @param bizType 业务类型。
     * @param bizId 业务ID。
     * @return 文件ID。
     */
    Long save(byte[] content, String fileName, String contentType, String purpose, String bizType, String bizId);

    /**
     * 读取模板源文件。
     *
     * @param fileId 文件ID。
     * @return 文件内容。
     */
    TemplateStoredFile read(Long fileId);
}
