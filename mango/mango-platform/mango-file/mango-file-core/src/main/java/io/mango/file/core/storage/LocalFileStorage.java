package io.mango.file.core.storage;

import io.mango.file.api.FileCode;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;

/**
 * 本地磁盘文件存储。
 */
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final FileProperties properties;

    @Override
    public boolean supports(String storageType) {
        return "LOCAL".equalsIgnoreCase(storageType);
    }

    @Override
    public void putObject(FileStorageConfig config, String objectName, InputStream inputStream, long contentLength, String contentType) throws IOException {
        Path target = resolvePath(config.getBucketName(), objectName);
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public FileObject getObject(FileStorageConfig config, String objectName) {
        Path target = resolvePath(config.getBucketName(), objectName);
        Require.isTrue(Files.exists(target) && Files.isRegularFile(target), FileCode.FILE_NOT_FOUND);
        try {
            String contentType = Files.probeContentType(target);
            return new FileObject(Files.newInputStream(target), Files.size(target), contentType);
        } catch (IOException e) {
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void removeObject(FileStorageConfig config, String objectName) {
        Path target = resolvePath(config.getBucketName(), objectName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void test(FileStorageConfig config) throws IOException {
        Path target = resolvePath(config.getBucketName(), ".mango-storage-test");
        Files.createDirectories(target.getParent());
    }

    @Override
    public Optional<String> presignedGetUrl(FileStorageConfig config, String objectName, String fileName, Duration expires) {
        return publicGetUrl(config, objectName, fileName);
    }

    @Override
    public Optional<String> publicGetUrl(FileStorageConfig config, String objectName, String fileName) {
        if (!StringUtils.hasText(objectName)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(config.getPublicEndpoint()) || !StringUtils.hasText(objectName)) {
            String bucket = StringUtils.hasText(config.getBucketName()) ? config.getBucketName().trim() : properties.getDefaultBucket();
            return Optional.of(localObjectUrl(bucket, objectName));
        }
        String endpoint = StringUtils.trimTrailingCharacter(config.getPublicEndpoint().trim(), '/');
        String bucket = StringUtils.hasText(config.getBucketName()) ? config.getBucketName().trim() : properties.getDefaultBucket();
        return Optional.of(endpoint + "/" + encode(bucket) + "/" + encodeObjectName(objectName));
    }

    private Path resolvePath(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : properties.getDefaultBucket();
        Path root = Path.of(properties.getLocal().getRootPath()).toAbsolutePath().normalize();
        Path resolved = root.resolve(bucket).resolve(objectName).normalize();
        Require.isTrue(resolved.startsWith(root), FileCode.FILE_ACCESS_DENIED);
        return resolved;
    }

    private String encodeObjectName(String objectName) {
        return URLEncoder.encode(objectName, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String localObjectUrl(String bucket, String objectName) {
        String path = properties.getLocal().getPublicPath();
        String prefix = StringUtils.hasText(path) ? path.trim() : "/api/file/local-objects";
        prefix = StringUtils.trimTrailingCharacter(prefix, '/');
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        return prefix + "/" + encode(bucket) + "/" + encodeObjectName(objectName);
    }
}
