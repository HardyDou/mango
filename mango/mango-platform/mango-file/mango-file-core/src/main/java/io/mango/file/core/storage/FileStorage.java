package io.mango.file.core.storage;

import io.mango.file.core.entity.FileStorageConfig;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * 文件存储抽象。
 */
public interface FileStorage {

    /** 是否支持该存储配置。 */
    boolean supports(String storageType);

    /** 保存文件对象。 */
    void putObject(FileStorageConfig config, String objectName, InputStream inputStream, long contentLength, String contentType) throws Exception;

    /** 读取文件对象。 */
    FileObject getObject(FileStorageConfig config, String objectName);

    /** 删除文件对象。 */
    void removeObject(FileStorageConfig config, String objectName);

    /** 测试存储配置连通性。 */
    void test(FileStorageConfig config) throws Exception;

    /** 生成浏览器可直接访问底层存储的限时地址。 */
    default Optional<String> presignedGetUrl(FileStorageConfig config, String objectName, String fileName, Duration expires) {
        return Optional.empty();
    }

    /** 生成浏览器可直接下载底层存储对象的限时地址。 */
    default Optional<String> presignedDownloadUrl(FileStorageConfig config, String objectName, String fileName, Duration expires) {
        return presignedGetUrl(config, objectName, fileName, expires);
    }

    /** 生成浏览器可直接访问底层存储的公开地址。 */
    default Optional<String> publicGetUrl(FileStorageConfig config, String objectName, String fileName) {
        return Optional.empty();
    }

    /** 生成浏览器可直接下载底层存储对象的公开地址。 */
    default Optional<String> publicDownloadUrl(FileStorageConfig config, String objectName, String fileName) {
        return publicGetUrl(config, objectName, fileName);
    }
}
