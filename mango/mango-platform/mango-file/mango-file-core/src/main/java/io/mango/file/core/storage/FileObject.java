package io.mango.file.core.storage;

import java.io.InputStream;

/**
 * 文件对象读取结果。
 *
 * @param inputStream 文件流
 * @param contentLength 文件大小
 * @param contentType 内容类型
 */
public record FileObject(InputStream inputStream, long contentLength, String contentType) {
}
