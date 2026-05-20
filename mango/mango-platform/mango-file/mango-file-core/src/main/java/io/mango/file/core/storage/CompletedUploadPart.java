package io.mango.file.core.storage;

/**
 * 已完成的对象存储分片。
 */
public record CompletedUploadPart(int partNumber, String etag) {
}
