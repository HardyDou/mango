package io.mango.file.core.storage;

import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 文件存储路由。
 */
@RequiredArgsConstructor
public class FileStorageRouter {

    private final List<FileStorage> storages;

    public void putObject(FileStorageConfig config, String objectName, InputStream inputStream, long contentLength, String contentType) throws Exception {
        storage(config).putObject(config, objectName, inputStream, contentLength, contentType);
    }

    public FileObject getObject(FileStorageConfig config, String objectName) {
        return storage(config).getObject(config, objectName);
    }

    public void removeObject(FileStorageConfig config, String objectName) {
        storage(config).removeObject(config, objectName);
    }

    public void test(FileStorageConfig config) throws Exception {
        storage(config).test(config);
    }

    public Optional<String> presignedGetUrl(FileStorageConfig config, String objectName, String fileName, Duration expires) {
        return storage(config).presignedGetUrl(config, objectName, fileName, expires);
    }

    public Optional<String> presignedDownloadUrl(FileStorageConfig config, String objectName, String fileName, Duration expires) {
        return storage(config).presignedDownloadUrl(config, objectName, fileName, expires);
    }

    public Optional<String> publicGetUrl(FileStorageConfig config, String objectName, String fileName) {
        return storage(config).publicGetUrl(config, objectName, fileName);
    }

    public Optional<String> publicDownloadUrl(FileStorageConfig config, String objectName, String fileName) {
        return storage(config).publicDownloadUrl(config, objectName, fileName);
    }

    private FileStorage storage(FileStorageConfig config) {
        Require.notNull(config, FileCode.STORAGE_CONFIG_NOT_FOUND);
        String storageType = config.getStorageType();
        Optional<FileStorage> storage = storages.stream()
                .filter(item -> item.supports(storageType))
                .findFirst();
        Require.isTrue(storage.isPresent(), FileCode.STORAGE_TYPE_UNSUPPORTED);
        return storage.get();
    }
}
