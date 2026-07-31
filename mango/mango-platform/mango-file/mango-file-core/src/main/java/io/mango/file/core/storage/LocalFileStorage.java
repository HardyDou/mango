package io.mango.file.core.storage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.file.api.enums.FileCode;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileStorageConfigEntity;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 本地磁盘文件存储。
 */
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "FileProperties is a Spring-managed configuration collaborator")
public class LocalFileStorage implements FileStorage {

    private final FileProperties properties;

    @Override
    public boolean supports(String storageType) {
        return "LOCAL".equalsIgnoreCase(storageType);
    }

    @Override
    public void putObject(FileStorageConfigEntity config, String objectName, InputStream inputStream, long contentLength, String contentType) throws IOException {
        Path target = resolvePath(config.getBucketName(), objectName);
        Files.createDirectories(requireParent(target));
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public FileObject getObject(FileStorageConfigEntity config, String objectName) {
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
    public void removeObject(FileStorageConfigEntity config, String objectName) {
        Path target = resolvePath(config.getBucketName(), objectName);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    @Override
    public void publishObject(FileStorageConfigEntity config, String stagingObjectName, String targetObjectName) {
        Path staging = resolvePath(config.getBucketName(), stagingObjectName);
        Path target = resolvePath(config.getBucketName(), targetObjectName);
        try {
            Files.createDirectories(requireParent(target));
            try {
                Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Require.fail(FileCode.FILE_STORE_FAILED);
        }
    }

    @Override
    public void test(FileStorageConfigEntity config) throws IOException {
        Path target = resolvePath(config.getBucketName(), ".mango-storage-test");
        Files.createDirectories(requireParent(target));
    }

    @Override
    public Optional<String> presignedGetUrl(FileStorageConfigEntity config, String objectName, String fileName, Duration expires) {
        return publicGetUrl(config, objectName, fileName);
    }

    @Override
    public Optional<String> presignedDownloadUrl(FileStorageConfigEntity config, String objectName, String fileName, Duration expires) {
        return publicDownloadUrl(config, objectName, fileName);
    }

    @Override
    public Optional<String> publicGetUrl(FileStorageConfigEntity config, String objectName, String fileName) {
        if (!StringUtils.hasText(objectName)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(config.getPublicEndpoint()) || !StringUtils.hasText(objectName)) {
            String bucket = StringUtils.hasText(config.getBucketName())
                    ? config.getBucketName().trim()
                    : properties.getDefaultBucket();
            return Optional.of(localObjectUrl(bucket, objectName));
        }
        String endpoint = publicEndpoint(config);
        String bucket = StringUtils.hasText(config.getBucketName())
                ? config.getBucketName().trim()
                : properties.getDefaultBucket();
        return Optional.of(endpoint + "/" + encode(bucket) + "/" + encodeObjectName(objectName));
    }

    @Override
    public Optional<String> publicDownloadUrl(FileStorageConfigEntity config, String objectName, String fileName) {
        return publicGetUrl(config, objectName, fileName)
                .map(this::withDownloadDisposition);
    }

    private String publicEndpoint(FileStorageConfigEntity config) {
        String endpoint = StringUtils.trimTrailingCharacter(config.getPublicEndpoint().trim(), '/');
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return (Integer.valueOf(1).equals(config.getSslEnabled()) ? "https://" : "http://") + endpoint;
    }

    private Path resolvePath(String bucketName, String objectName) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : properties.getDefaultBucket();
        Path root = Path.of(properties.getLocal().getRootPath()).toAbsolutePath().normalize();
        Path resolved = root.resolve(bucket).resolve(objectName).normalize();
        Require.isTrue(resolved.startsWith(root), FileCode.FILE_ACCESS_DENIED);
        return resolved;
    }

    private static Path requireParent(Path target) {
        return Objects.requireNonNull(target.getParent(), "Resolved local storage path must have a parent");
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
        String prefix = StringUtils.hasText(path) ? path.trim() : "/file/local-objects";
        prefix = StringUtils.trimTrailingCharacter(prefix, '/');
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        return prefix + "/" + encode(bucket) + "/" + encodeObjectName(objectName);
    }

    private String withDownloadDisposition(String url) {
        return url + (url.contains("?") ? "&" : "?") + "download=1";
    }
}
