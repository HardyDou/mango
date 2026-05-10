package io.mango.file.core.storage;

import io.mango.file.api.FileCode;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.common.result.Require;
import org.springframework.util.StringUtils;

/**
 * 云存储基础校验。
 */
abstract class AbstractCloudFileStorage implements FileStorage {

    protected void requireBucket(FileStorageConfig config) {
        Require.notBlank(config.getBucketName(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "存储桶不能为空");
    }

    protected void requireEndpoint(FileStorageConfig config) {
        Require.notBlank(config.getEndpoint(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "接入地址不能为空");
    }

    protected void requireAccessSecret(FileStorageConfig config) {
        Require.notBlank(config.getAccessKey(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "AccessKey不能为空");
        Require.notBlank(config.getSecretKey(), FileCode.STORAGE_CONFIG_INVALID.getCode(), "SecretKey不能为空");
    }

    protected String regionOrDefault(FileStorageConfig config, String defaultRegion) {
        return StringUtils.hasText(config.getRegion()) ? config.getRegion().trim() : defaultRegion;
    }

    protected boolean enabled(Integer value) {
        return Integer.valueOf(1).equals(value);
    }
}
