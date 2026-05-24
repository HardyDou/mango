package io.mango.file.preview.core.service.model;

import java.io.InputStream;

/**
 * 文件预览源文件。
 *
 * @param inputStream 文件流。
 * @param fileName 文件名。
 * @param contentType 内容类型。
 * @param contentLength 文件大小。
 */
public record FilePreviewSource(InputStream inputStream, String fileName, String contentType, long contentLength) {
}
