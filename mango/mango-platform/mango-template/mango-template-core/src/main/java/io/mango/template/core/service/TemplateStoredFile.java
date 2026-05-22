package io.mango.template.core.service;

import java.io.InputStream;

/**
 * 模板模块读取到的文件。
 *
 * @param inputStream 文件流
 * @param fileName 文件名
 * @param contentType 内容类型
 * @param contentLength 文件大小
 */
public record TemplateStoredFile(InputStream inputStream, String fileName, String contentType, long contentLength) {
}
