package io.mango.file.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.file.core.mapper.FileStorageConfigMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 文件存储配置资源处理器。
 */
@Component
@RequiredArgsConstructor
public class FileStorageConfigResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "file_storage_config";
    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final FileStorageConfigMapper storageConfigMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.FILE_STORAGE_CONFIG;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("configName")
                .requiredField("storageType")
                .requiredField("bucketName")
                .fieldDescription("storageConfigId", "文件存储配置稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("configName", "存储配置名称，全局唯一。")
                .fieldDescription("storageType", "存储类型：LOCAL、MINIO、AWS_S3、ALIYUN_OSS、TENCENT_COS、QINIU_KODO。")
                .fieldDescription("endpoint", "接入地址。")
                .fieldDescription("publicEndpoint", "公开访问地址。")
                .fieldDescription("region", "区域。")
                .fieldDescription("bucketName", "存储桶名称。")
                .fieldDescription("storagePath", "存储路径前缀，默认空字符串。")
                .fieldDescription("accessKey", "访问密钥 AccessKey。")
                .fieldDescription("secretKey", "访问密钥 SecretKey。")
                .fieldDescription("pathStyleAccess", "是否使用 Path Style 访问，默认 0。")
                .fieldDescription("sslEnabled", "是否启用 HTTPS，默认 0。")
                .fieldDescription("active", "是否默认启用，默认 0。")
                .fieldDescription("status", "状态：1 启用，0 停用，默认 1。")
                .fieldDescription("remark", "备注。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        StoragePayload payload = StoragePayload.from(resource);
        FileStorageConfig entity = find(payload.configName());
        if (entity == null) {
            entity = new FileStorageConfig();
            entity.setId(payload.storageConfigId());
            entity.setTenantId(payload.tenantId());
            entity.setConfigName(payload.configName());
            apply(entity, payload);
            storageConfigMapper.insert(entity);
        } else {
            apply(entity, payload);
            storageConfigMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "File storage config synced: " + payload.configName());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        FileStorageConfig entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "File storage config not found");
        }
        entity.setStatus(DISABLED);
        entity.setActive(DISABLED);
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        storageConfigMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "File storage config disabled: " + entity.getConfigName());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        FileStorageConfig entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "File storage config not found");
        }
        storageConfigMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "File storage config deleted: " + entity.getConfigName());
    }

    private void apply(FileStorageConfig entity, StoragePayload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(payload.tenantId());
        entity.setConfigName(payload.configName());
        entity.setStorageType(payload.storageType());
        entity.setEndpoint(payload.endpoint());
        entity.setPublicEndpoint(payload.publicEndpoint());
        entity.setRegion(payload.region());
        entity.setBucketName(payload.bucketName());
        entity.setStoragePath(payload.storagePath());
        entity.setAccessKey(payload.accessKey());
        entity.setSecretKey(payload.secretKey());
        entity.setPathStyleAccess(payload.pathStyleAccess());
        entity.setSslEnabled(payload.sslEnabled());
        entity.setActive(payload.active());
        entity.setStatus(payload.status());
        entity.setRemark(payload.remark());
        if (entity.getCreatedTime() == null) {
            entity.setCreatedTime(now);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
    }

    private FileStorageConfig resolve(ResourceDeclaration resource) {
        String configName = fieldText(resource, "configName", false);
        if (StringUtils.hasText(configName)) {
            FileStorageConfig entity = find(configName.trim());
            if (entity != null) {
                return entity;
            }
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId != null) {
            return storageConfigMapper.selectById(targetId);
        }
        Long storageConfigId = fieldLong(resource, "storageConfigId", false, null);
        return storageConfigId == null ? null : storageConfigMapper.selectById(storageConfigId);
    }

    private FileStorageConfig find(String configName) {
        return storageConfigMapper.selectOne(new LambdaQueryWrapper<FileStorageConfig>()
                .eq(FileStorageConfig::getConfigName, configName)
                .last("limit 1"));
    }

    private record StoragePayload(Long storageConfigId, Long tenantId, String configName, String storageType,
                                  String endpoint, String publicEndpoint, String region, String bucketName,
                                  String storagePath, String accessKey, String secretKey,
                                  Integer pathStyleAccess, Integer sslEnabled, Integer active,
                                  Integer status, String remark) {

        private static StoragePayload from(ResourceDeclaration resource) {
            return new StoragePayload(
                    fieldLong(resource, "storageConfigId", false, Long.valueOf(resource.getId())),
                    fieldLong(resource, "tenantId", false, DEFAULT_TENANT_ID),
                    requiredText(fieldValue(resource, "configName", true),
                            "FILE_STORAGE_CONFIG configName is required").trim(),
                    requiredText(fieldValue(resource, "storageType", true),
                            "FILE_STORAGE_CONFIG storageType is required").trim().toUpperCase(),
                    fieldText(resource, "endpoint", false),
                    fieldText(resource, "publicEndpoint", false),
                    fieldText(resource, "region", false),
                    requiredText(fieldValue(resource, "bucketName", true),
                            "FILE_STORAGE_CONFIG bucketName is required").trim(),
                    defaultText(fieldText(resource, "storagePath", false), ""),
                    fieldText(resource, "accessKey", false),
                    fieldText(resource, "secretKey", false),
                    fieldInt(resource, "pathStyleAccess", false, DISABLED),
                    fieldInt(resource, "sslEnabled", false, DISABLED),
                    fieldInt(resource, "active", false, DISABLED),
                    fieldInt(resource, "status", false, ENABLED),
                    fieldText(resource, "remark", false)
            );
        }
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("FILE_STORAGE_CONFIG field is required: " + name);
        }
        return value;
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Integer fieldInt(ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    private static String requiredText(Object value, String message) {
        String text = toText(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(message);
        }
        return text;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("FILE_STORAGE_CONFIG long value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer toInt(Object value, boolean required, Integer defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("FILE_STORAGE_CONFIG int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
