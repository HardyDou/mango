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
        if (!StringUtils.hasText(config.getPublicEndpoint()) || !StringUtils.hasText(objectName)) {
            return Optional.empty();
        }
        String endpoint = StringUtils.trimTrailingCharacter(config.getPublicEndpoint().trim(), '/');
        return Optional.of(endpoint + "/" + encodeObjectName(objectName));
    }

    protected String encodeObjectName(String objectName) {
        return URLEncoder.encode(objectName, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");
    }
}
