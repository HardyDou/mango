package io.mango.file.api.vo;

import java.io.InputStream;

/**
 * 文件下载结果。
 *
 * @param inputStream 文件流
 * @param fileName 文件名
 * @param contentType 内容类型
 * @param contentLength 文件大小
 */
public record FileDownloadVO(InputStream inputStream, String fileName, String contentType, long contentLength) {
}
