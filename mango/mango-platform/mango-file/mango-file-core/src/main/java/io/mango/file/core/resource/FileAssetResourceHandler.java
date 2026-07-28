package io.mango.file.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.file.api.enums.FileObjectStatus;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecordEntity;
import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.mapper.FileObjectMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.mapper.FileStorageConfigMapper;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 将业务模块随 Jar 发布的二进制资产幂等写入文件服务。
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The handler intentionally retains Spring-managed storage collaborators"))
public class FileAssetResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "file_record";
    private static final String ASSET_CLASSPATH_PREFIX = "classpath:META-INF/mango/assets/";
    private static final String MANAGED_OBJECT_PREFIX = "mango-assets/";
    private static final String STAGING_PREFIX = ".mango-staging/resources/";
    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int ENABLED = 1;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_OBJECT_NAME_LENGTH = 500;

    private final FileStorageConfigMapper storageConfigMapper;
    private final FileObjectMapper fileObjectMapper;
    private final FileRecordMapper fileRecordMapper;
    private final FileStorageRouter fileStorageRouter;
    private final ResourceLoader resourceLoader;

    @Override
    public String resourceType() {
        return ResourceTypes.FILE_ASSET;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(ResourceTypes.FILE_STORAGE_CONFIG);
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("fileId")
                .requiredField("storageConfigId")
                .requiredField("objectName")
                .requiredField("fileName")
                .requiredField("sha256")
                .requiredField("content")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("fileId", "业务引用的稳定 file_record ID。")
                .fieldDescription("storageConfigId", "目标文件存储配置稳定 ID。")
                .fieldDescription("objectName", "mango-assets/ 前缀下的稳定对象位置。")
                .fieldDescription("fileName", "对业务展示的原始文件名。")
                .fieldDescription("sha256", "classpath 制品的 SHA-256。")
                .fieldDescription("content", "FILE 类型的 classpath 二进制制品。")
                .fieldDescription("purpose", "文件用途，默认 managed-asset。")
                .fieldDescription("accessLevel", "访问级别，默认 INTERNAL。")
                .fieldDescription("bizType", "业务类型，默认 MANGO_RESOURCE。")
                .fieldDescription("bizId", "业务 ID，默认 Resource bizKey 或资源 ID。")
                .fieldDescription("bizMeta", "业务扩展 JSON。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration declaration) {
        AssetPayload payload = AssetPayload.from(declaration, resourceLoader);
        FileStorageConfigEntity storageConfig = requireStorageConfig(payload);
        FileRecordEntity record = fileRecordMapper.selectById(payload.fileId());
        validateStableIdentity(record, payload);

        FileObjectEntity fileObject = resolveFileObject(record, payload, storageConfig);
        boolean metadataMatches = matches(fileObject, payload, storageConfig);
        boolean objectMatches = metadataMatches
                && storedObjectMatches(storageConfig, payload.objectName(), payload);
        if (!objectMatches) {
            publish(payload, storageConfig, declaration.getId());
        }

        fileObject = persistFileObject(fileObject, payload, storageConfig);
        record = persistFileRecord(record, fileObject, payload, declaration);
        return ResourceSyncResult.of(record.getId(), TARGET_TABLE,
                objectMatches ? "File asset verified: " + payload.objectName()
                        : "File asset published: " + payload.objectName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration declaration) {
        Long fileId = fieldLong(declaration, "fileId", false, null);
        FileRecordEntity record = fileId == null ? null : fileRecordMapper.selectById(fileId);
        if (record == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "File asset not found");
        }
        record.setStatus(FileRecordStatus.ARCHIVED.value());
        record.setArchived(ENABLED);
        touchUpdated(record);
        fileRecordMapper.updateById(record);
        return ResourceSyncResult.of(record.getId(), TARGET_TABLE,
                "File asset archived: " + record.getObjectName());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration declaration) {
        return disable(declaration);
    }

    private FileStorageConfigEntity requireStorageConfig(AssetPayload payload) {
        FileStorageConfigEntity config = storageConfigMapper.selectById(payload.storageConfigId());
        if (config == null || !Integer.valueOf(ENABLED).equals(config.getStatus())) {
            throw new IllegalStateException("FILE_ASSET storage config is unavailable: "
                    + payload.storageConfigId());
        }
        return config;
    }

    private void validateStableIdentity(FileRecordEntity record, AssetPayload payload) {
        if (record == null) {
            return;
        }
        if (!Objects.equals(record.getTenantIdAsLong(), payload.tenantId())
                || !Objects.equals(record.getStorageConfigId(), payload.storageConfigId())
                || !Objects.equals(record.getObjectName(), payload.objectName())) {
            throw new IllegalStateException("FILE_ASSET stable identity drift detected for fileId="
                    + payload.fileId());
        }
    }

    private FileObjectEntity resolveFileObject(FileRecordEntity record, AssetPayload payload,
                                                FileStorageConfigEntity storageConfig) {
        if (record != null && record.getObjectId() != null) {
            FileObjectEntity fileObject = fileObjectMapper.selectById(record.getObjectId());
            if (fileObject != null) {
                return fileObject;
            }
        }
        return fileObjectMapper.selectOne(new LambdaQueryWrapper<FileObjectEntity>()
                .eq(FileObjectEntity::getStorageConfigId, payload.storageConfigId())
                .eq(FileObjectEntity::getBucketName, storageConfig.getBucketName())
                .eq(FileObjectEntity::getObjectName, payload.objectName())
                .last("limit 1"));
    }

    private boolean matches(FileObjectEntity fileObject, AssetPayload payload,
                            FileStorageConfigEntity storageConfig) {
        return fileObject != null
                && Objects.equals(fileObject.getTenantIdAsLong(), payload.tenantId())
                && Objects.equals(fileObject.getStorageConfigId(), payload.storageConfigId())
                && Objects.equals(fileObject.getBucketName(), storageConfig.getBucketName())
                && Objects.equals(fileObject.getObjectName(), payload.objectName())
                && Objects.equals(fileObject.getFileHash(), payload.sha256())
                && Objects.equals(fileObject.getFileSize(), payload.contentLength())
                && Integer.valueOf(FileObjectStatus.COMPLETED.value()).equals(fileObject.getStatus());
    }

    private void publish(AssetPayload payload, FileStorageConfigEntity storageConfig, String resourceId) {
        String stagingObjectName = STAGING_PREFIX + safeSegment(resourceId) + "/" + payload.sha256();
        try (InputStream input = payload.content().getInputStream()) {
            fileStorageRouter.putObject(storageConfig, stagingObjectName, input,
                    payload.contentLength(), payload.contentType());
            if (!storedObjectMatches(storageConfig, stagingObjectName, payload)) {
                fileStorageRouter.removeObject(storageConfig, stagingObjectName);
                throw new IllegalStateException("FILE_ASSET staging checksum mismatch: " + stagingObjectName);
            }
            fileStorageRouter.publishObject(storageConfig, stagingObjectName, payload.objectName());
        } catch (Exception e) {
            throw new IllegalStateException("Publish FILE_ASSET failed: " + payload.objectName(), e);
        }
    }

    private boolean storedObjectMatches(FileStorageConfigEntity config, String objectName, AssetPayload payload) {
        try {
            FileObject object = fileStorageRouter.getObject(config, objectName);
            if (object.contentLength() != payload.contentLength()) {
                object.inputStream().close();
                return false;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = object.inputStream();
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(OutputStream.nullOutputStream());
            }
            return payload.sha256().equals(HexFormat.of().formatHex(digest.digest()));
        } catch (RuntimeException | IOException | NoSuchAlgorithmException ignored) {
            return false;
        }
    }

    private FileObjectEntity persistFileObject(FileObjectEntity entity, AssetPayload payload,
                                               FileStorageConfigEntity storageConfig) {
        boolean insert = entity == null;
        FileObjectEntity target = insert ? new FileObjectEntity() : entity;
        target.setTenantId(payload.tenantId());
        target.setStorageConfigId(payload.storageConfigId());
        target.setStorageType(storageConfig.getStorageType());
        target.setBucketName(storageConfig.getBucketName());
        target.setObjectName(payload.objectName());
        target.setFileHash(payload.sha256());
        target.setFileSize(payload.contentLength());
        target.setContentType(payload.contentType());
        target.setStatus(FileObjectStatus.COMPLETED.value());
        target.setRefCount(1L);
        touch(target, insert);
        if (insert) {
            fileObjectMapper.insert(target);
        } else {
            fileObjectMapper.updateById(target);
        }
        return target;
    }

    private FileRecordEntity persistFileRecord(FileRecordEntity entity, FileObjectEntity fileObject,
                                               AssetPayload payload, ResourceDeclaration declaration) {
        boolean insert = entity == null;
        FileRecordEntity target = insert ? new FileRecordEntity() : entity;
        target.setId(payload.fileId());
        target.setTenantId(payload.tenantId());
        target.setBizType(payload.bizType());
        target.setBizId(payload.bizId(declaration));
        target.setPurpose(payload.purpose());
        target.setBizMeta(payload.bizMeta());
        target.setDirectoryId(0L);
        target.setAccessLevel(payload.accessLevel());
        target.setObjectId(fileObject.getId());
        target.setStorageType(fileObject.getStorageType());
        target.setStorageConfigId(fileObject.getStorageConfigId());
        target.setBucketName(fileObject.getBucketName());
        target.setObjectName(fileObject.getObjectName());
        target.setFileName(payload.fileName());
        target.setFileExt(fileExtension(payload.fileName()));
        target.setFileSize(payload.contentLength());
        target.setContentType(payload.contentType());
        target.setFileHash(payload.sha256());
        target.setStatus(FileRecordStatus.COMPLETED.value());
        target.setArchived(0);
        touch(target, insert);
        if (insert) {
            fileRecordMapper.insert(target);
        } else {
            fileRecordMapper.updateById(target);
        }
        return target;
    }

    private static void touch(FileObjectEntity entity, boolean insert) {
        LocalDateTime now = LocalDateTime.now();
        if (insert) {
            entity.setCreatedTime(now);
            entity.setCreatedAt(now);
        }
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
    }

    private static void touch(FileRecordEntity entity, boolean insert) {
        LocalDateTime now = LocalDateTime.now();
        if (insert) {
            entity.setCreatedTime(now);
            entity.setCreatedAt(now);
        }
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
    }

    private static void touchUpdated(FileRecordEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
    }

    private static String fileExtension(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator < 0 || separator == fileName.length() - 1
                ? null : fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private static String safeSegment(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim() : "unknown";
        return normalized.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private record AssetPayload(Long tenantId, Long fileId, Long storageConfigId, String objectName,
                                String fileName, String sha256, Resource content, long contentLength,
                                String contentType, String purpose, String accessLevel, String bizType,
                                String bizId, String bizMeta) {

        private static AssetPayload from(ResourceDeclaration declaration, ResourceLoader resourceLoader) {
            Long tenantId = fieldLong(declaration, "tenantId", false, DEFAULT_TENANT_ID);
            Long fileId = fieldLong(declaration, "fileId", true, null);
            Long storageConfigId = fieldLong(declaration, "storageConfigId", true, null);
            validatePositiveIds(tenantId, fileId, storageConfigId);
            String objectName = requiredText(declaration, "objectName");
            validateObjectName(objectName);
            String fileName = validatedFileName(declaration);
            String sha256 = validatedSha256(declaration);
            ResourceField contentField = requiredContentField(declaration);
            Resource content = readableContent(resourceLoader, contentField);
            long contentLength = contentLength(content);
            String actualHash = sha256(content);
            if (!sha256.equals(actualHash)) {
                throw new IllegalStateException("FILE_ASSET sha256 mismatch: " + contentField.getLocation());
            }
            String accessLevel = validatedAccessLevel(declaration);
            return new AssetPayload(tenantId, fileId, storageConfigId, objectName, fileName, sha256,
                    content, contentLength, defaultText(contentField.getMediaType(), "application/octet-stream"),
                    defaultText(fieldText(declaration, "purpose", false), "managed-asset"),
                    accessLevel,
                    defaultText(fieldText(declaration, "bizType", false), "MANGO_RESOURCE"),
                    fieldText(declaration, "bizId", false), fieldText(declaration, "bizMeta", false));
        }

        private String bizId(ResourceDeclaration declaration) {
            if (StringUtils.hasText(bizId)) {
                return bizId.trim();
            }
            return StringUtils.hasText(declaration.getBizKey())
                    ? declaration.getBizKey().trim() : declaration.getId();
        }

        private static void validateObjectName(String objectName) {
            if (!objectName.startsWith(MANAGED_OBJECT_PREFIX) || objectName.startsWith("/")
                    || objectName.contains("..") || objectName.contains("\\")
                    || objectName.length() > MAX_OBJECT_NAME_LENGTH) {
                throw new IllegalStateException("FILE_ASSET objectName must be a safe path under "
                        + MANAGED_OBJECT_PREFIX);
            }
        }

        private static void validatePositiveIds(Long tenantId, Long fileId, Long storageConfigId) {
            if (tenantId == null || tenantId <= 0 || fileId == null || fileId <= 0
                    || storageConfigId == null || storageConfigId <= 0) {
                throw new IllegalStateException("FILE_ASSET tenantId, fileId and storageConfigId must be positive");
            }
        }

        private static String validatedFileName(ResourceDeclaration declaration) {
            String fileName = requiredText(declaration, "fileName");
            if (fileName.length() > MAX_FILE_NAME_LENGTH) {
                throw new IllegalStateException("FILE_ASSET fileName must not exceed "
                        + MAX_FILE_NAME_LENGTH + " characters");
            }
            return fileName;
        }

        private static String validatedSha256(ResourceDeclaration declaration) {
            String sha256 = requiredText(declaration, "sha256").toLowerCase(Locale.ROOT);
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalStateException("FILE_ASSET sha256 must contain 64 lowercase hex characters");
            }
            return sha256;
        }

        private static ResourceField requiredContentField(ResourceDeclaration declaration) {
            ResourceField contentField = declaration.getFields().get("content");
            if (contentField == null || contentField.getType() != ResourceFieldType.FILE
                    || !StringUtils.hasText(contentField.getLocation())
                    || !contentField.getLocation().startsWith(ASSET_CLASSPATH_PREFIX)
                    || contentField.getLocation().contains("..")) {
                throw new IllegalStateException("FILE_ASSET content must use " + ASSET_CLASSPATH_PREFIX);
            }
            return contentField;
        }

        private static Resource readableContent(ResourceLoader resourceLoader, ResourceField contentField) {
            Resource content = resourceLoader.getResource(contentField.getLocation());
            if (!content.exists() || !content.isReadable()) {
                throw new IllegalStateException("FILE_ASSET content is not readable: "
                        + contentField.getLocation());
            }
            return content;
        }

        private static String validatedAccessLevel(ResourceDeclaration declaration) {
            String accessLevel = defaultText(fieldText(declaration, "accessLevel", false), "INTERNAL")
                    .toUpperCase(Locale.ROOT);
            if (!List.of("PRIVATE", "PUBLIC_READ", "INTERNAL").contains(accessLevel)) {
                throw new IllegalStateException("FILE_ASSET accessLevel is invalid: " + accessLevel);
            }
            return accessLevel;
        }

        private static long contentLength(Resource content) {
            try {
                long length = content.contentLength();
                if (length <= 0) {
                    throw new IllegalStateException("FILE_ASSET content must not be empty");
                }
                return length;
            } catch (IOException e) {
                throw new IllegalStateException("Read FILE_ASSET content length failed", e);
            }
        }

        private static String sha256(Resource content) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = content.getInputStream();
                     DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                    digestInput.transferTo(OutputStream.nullOutputStream());
                }
                return HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new IllegalStateException("Calculate FILE_ASSET sha256 failed", e);
            }
        }
    }

    private static String requiredText(ResourceDeclaration declaration, String name) {
        String value = fieldText(declaration, name, true);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("FILE_ASSET field is required: " + name);
        }
        return value.trim();
    }

    private static String fieldText(ResourceDeclaration declaration, String name, boolean required) {
        Object value = fieldValue(declaration, name, required);
        return value == null ? null : String.valueOf(value);
    }

    private static Long fieldLong(ResourceDeclaration declaration, String name, boolean required,
                                  Long defaultValue) {
        Object value = fieldValue(declaration, name, required);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return defaultValue;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private static Object fieldValue(ResourceDeclaration declaration, String name, boolean required) {
        ResourceField field = declaration.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("FILE_ASSET field is required: " + name);
        }
        return value;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
