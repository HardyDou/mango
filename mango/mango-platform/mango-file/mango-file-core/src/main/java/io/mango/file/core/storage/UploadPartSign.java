package io.mango.file.core.storage;

/**
 * 对象存储分片上传签名。
 */
public record UploadPartSign(int partNumber, String uploadUrl, String method, long expireSeconds) {
}
