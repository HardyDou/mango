package io.mango.file.core.storage;

import io.mango.file.core.entity.FileStorageConfig;

import java.io.InputStream;

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
}
