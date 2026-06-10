package io.mango.file.core.storage;

import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 云存储基础校验。
 */
abstract class AbstractCloudFileStorage implements FileStorage {

    protected void requireBucket(FileStorageConfig config) {
        Require.notBlank(config.getBucketName(), FileCode.STORAGE_CONFIG_INVALID);
    }

    protected void requireEndpoint(FileStorageConfig config) {
        Require.notBlank(config.getEndpoint(), FileCode.STORAGE_CONFIG_INVALID);
    }

    protected void requireAccessSecret(FileStorageConfig config) {
        Require.notBlank(config.getAccessKey(), FileCode.STORAGE_CONFIG_INVALID);
        Require.notBlank(config.getSecretKey(), FileCode.STORAGE_CONFIG_INVALID);
    }

    protected String regionOrDefault(FileStorageConfig config, String defaultRegion) {
        return StringUtils.hasText(config.getRegion()) ? config.getRegion().trim() : defaultRegion;
    }

    protected boolean enabled(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    protected Optional<String> publicObjectUrl(FileStorageConfig config, String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return Optional.empty();
        }
        String endpoint = publicAccessEndpoint(config);
        if (!StringUtils.hasText(endpoint)) {
            return Optional.empty();
        }
        String path = encodeObjectName(objectName);
        if (enabled(config.getPathStyleAccess()) && StringUtils.hasText(config.getBucketName())) {
            path = encode(config.getBucketName().trim()) + "/" + path;
        }
        return Optional.of(endpoint + "/" + path);
    }

    protected String publicAccessEndpoint(FileStorageConfig config) {
        String endpoint = StringUtils.hasText(config.getPublicEndpoint())
                ? config.getPublicEndpoint().trim()
                : config.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            return null;
        }
        String normalized = StringUtils.trimTrailingCharacter(endpoint.trim(), '/');
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        String scheme = enabled(config.getSslEnabled()) ? "https://" : "http://";
        return scheme + normalized;
    }

    protected String encodeObjectName(String objectName) {
        return URLEncoder.encode(objectName, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");
    }

    protected String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
