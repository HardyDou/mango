package io.mango.file.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.file.core.entity.FileSettings;
import io.mango.file.core.mapper.FileSettingsMapper;
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
 * 文件中心运行时配置资源处理器。
 */
@Component
@RequiredArgsConstructor
public class FileSettingsResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "file_settings";
    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final FileSettingsMapper settingsMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.FILE_SETTINGS;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .fieldDescription("settingsId", "文件中心配置稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("tenantId", "租户 ID，默认 1；同一租户只允许一条配置。")
                .fieldDescription("maxSize", "单文件最大大小，单位字节，默认 104857600。")
                .fieldDescription("allowedExtensions", "允许上传扩展名，逗号分隔；为空表示不限制。")
                .fieldDescription("blockedExtensions", "禁止上传扩展名，默认 exe,bat,cmd,sh,jar。")
                .fieldDescription("defaultAccessLevel", "默认访问级别，默认 PRIVATE。")
                .fieldDescription("duplicateNameStrategy", "重名处理策略，默认 REJECT。")
                .fieldDescription("duplicateCheckDirectoryScoped", "是否按目录隔离重名，默认 1。")
                .fieldDescription("objectNameStrategy", "对象命名策略，默认 DATE_UUID。")
                .fieldDescription("instantUploadEnabled", "是否启用秒传，默认 1。")
                .fieldDescription("instantUploadScope", "秒传匹配范围，默认 TENANT。")
                .fieldDescription("contentTypeCheckEnabled", "是否校验内容类型，默认 1。")
                .fieldDescription("allowedContentTypes", "允许上传内容类型，逗号分隔；为空表示不限制。")
                .fieldDescription("blockedContentTypes", "禁止上传内容类型。")
                .fieldDescription("directUploadEnabled", "是否启用客户端直传，默认 0。")
                .fieldDescription("directUploadExpireSeconds", "直传签名有效期，默认 900。")
                .fieldDescription("accessTokenEnabled", "是否启用限时访问令牌，默认 0。")
                .fieldDescription("publicReadRequiresToken", "公开读取是否强制签名访问，默认 0。")
                .fieldDescription("accessMode", "文件访问模式，默认 PROXY。")
                .fieldDescription("accessTokenExpireSeconds", "访问令牌有效期，默认 600。")
                .fieldDescription("previewProviderUrl", "外部预览服务地址。")
                .fieldDescription("previewExpireSeconds", "预览访问有效期，默认 600。")
                .fieldDescription("previewExternalExtensions", "外部预览扩展名，逗号分隔。")
                .fieldDescription("archiveRetainEnabled", "是否保留归档记录，默认 1。")
                .fieldDescription("archiveRetainDays", "归档记录保留天数，默认 180。")
                .fieldDescription("archiveRestoreEnabled", "是否允许恢复归档，默认 0。")
                .fieldDescription("physicalDeleteEnabled", "是否删除物理对象，默认 0。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        SettingsPayload payload = SettingsPayload.from(resource);
        FileSettings entity = find(payload.tenantId());
        if (entity == null) {
            entity = new FileSettings();
            entity.setId(payload.settingsId());
            entity.setTenantId(payload.tenantId());
            apply(entity, payload);
            settingsMapper.insert(entity);
        } else {
            apply(entity, payload);
            settingsMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "File settings synced: " + payload.tenantId());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        FileSettings entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "File settings not found");
        }
        LocalDateTime now = LocalDateTime.now();
        entity.setInstantUploadEnabled(DISABLED);
        entity.setDirectUploadEnabled(DISABLED);
        entity.setAccessTokenEnabled(DISABLED);
        entity.setArchiveRestoreEnabled(DISABLED);
        entity.setPhysicalDeleteEnabled(DISABLED);
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
        settingsMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "File settings disabled: " + entity.getTenantId());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        FileSettings entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "File settings not found");
        }
        settingsMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "File settings deleted: " + entity.getTenantId());
    }

    private void apply(FileSettings entity, SettingsPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setMaxSize(payload.maxSize());
        entity.setAllowedExtensions(payload.allowedExtensions());
        entity.setBlockedExtensions(payload.blockedExtensions());
        entity.setDefaultAccessLevel(payload.defaultAccessLevel());
        entity.setDuplicateNameStrategy(payload.duplicateNameStrategy());
        entity.setDuplicateCheckDirectoryScoped(payload.duplicateCheckDirectoryScoped());
        entity.setObjectNameStrategy(payload.objectNameStrategy());
        entity.setInstantUploadEnabled(payload.instantUploadEnabled());
        entity.setInstantUploadScope(payload.instantUploadScope());
        entity.setContentTypeCheckEnabled(payload.contentTypeCheckEnabled());
        entity.setAllowedContentTypes(payload.allowedContentTypes());
        entity.setBlockedContentTypes(payload.blockedContentTypes());
        entity.setDirectUploadEnabled(payload.directUploadEnabled());
        entity.setDirectUploadExpireSeconds(payload.directUploadExpireSeconds());
        entity.setAccessTokenEnabled(payload.accessTokenEnabled());
        entity.setPublicReadRequiresToken(payload.publicReadRequiresToken());
        entity.setAccessMode(payload.accessMode());
        entity.setAccessTokenExpireSeconds(payload.accessTokenExpireSeconds());
        entity.setPreviewProviderUrl(payload.previewProviderUrl());
        entity.setPreviewExpireSeconds(payload.previewExpireSeconds());
        entity.setPreviewExternalExtensions(payload.previewExternalExtensions());
        entity.setArchiveRetainEnabled(payload.archiveRetainEnabled());
        entity.setArchiveRetainDays(payload.archiveRetainDays());
        entity.setArchiveRestoreEnabled(payload.archiveRestoreEnabled());
        entity.setPhysicalDeleteEnabled(payload.physicalDeleteEnabled());
        if (entity.getCreatedTime() == null) {
            entity.setCreatedTime(now);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
    }

    private FileSettings resolve(ResourceDeclaration resource) {
        Long tenantId = fieldLong(resource, "tenantId", false, DEFAULT_TENANT_ID);
        FileSettings entity = find(tenantId);
        if (entity != null) {
            return entity;
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId != null) {
            return settingsMapper.selectById(targetId);
        }
        Long settingsId = fieldLong(resource, "settingsId", false, null);
        return settingsId == null ? null : settingsMapper.selectById(settingsId);
    }

    private FileSettings find(Long tenantId) {
        return settingsMapper.selectOne(new LambdaQueryWrapper<FileSettings>()
                .eq(FileSettings::getTenantId, tenantId)
                .last("limit 1"));
    }

    private record SettingsPayload(Long settingsId, Long tenantId, Long maxSize, String allowedExtensions,
                                   String blockedExtensions, String defaultAccessLevel,
                                   String duplicateNameStrategy, Integer duplicateCheckDirectoryScoped,
                                   String objectNameStrategy, Integer instantUploadEnabled,
                                   String instantUploadScope, Integer contentTypeCheckEnabled,
                                   String allowedContentTypes, String blockedContentTypes,
                                   Integer directUploadEnabled, Long directUploadExpireSeconds,
                                   Integer accessTokenEnabled, Integer publicReadRequiresToken,
                                   String accessMode, Long accessTokenExpireSeconds,
                                   String previewProviderUrl, Long previewExpireSeconds,
                                   String previewExternalExtensions, Integer archiveRetainEnabled,
                                   Integer archiveRetainDays, Integer archiveRestoreEnabled,
                                   Integer physicalDeleteEnabled) {

        private static SettingsPayload from(ResourceDeclaration resource) {
            return new SettingsPayload(
                    fieldLong(resource, "settingsId", false, Long.valueOf(resource.getId())),
                    fieldLong(resource, "tenantId", false, DEFAULT_TENANT_ID),
                    fieldLong(resource, "maxSize", false, 104857600L),
                    fieldText(resource, "allowedExtensions", false),
                    defaultText(fieldText(resource, "blockedExtensions", false), "exe,bat,cmd,sh,jar"),
                    defaultText(fieldText(resource, "defaultAccessLevel", false), "PRIVATE").toUpperCase(),
                    defaultText(fieldText(resource, "duplicateNameStrategy", false), "REJECT").toUpperCase(),
                    fieldInt(resource, "duplicateCheckDirectoryScoped", false, ENABLED),
                    defaultText(fieldText(resource, "objectNameStrategy", false), "DATE_UUID").toUpperCase(),
                    fieldInt(resource, "instantUploadEnabled", false, ENABLED),
                    defaultText(fieldText(resource, "instantUploadScope", false), "TENANT").toUpperCase(),
                    fieldInt(resource, "contentTypeCheckEnabled", false, ENABLED),
                    fieldText(resource, "allowedContentTypes", false),
                    defaultText(fieldText(resource, "blockedContentTypes", false),
                            "application/x-msdownload,application/x-sh"),
                    fieldInt(resource, "directUploadEnabled", false, DISABLED),
                    fieldLong(resource, "directUploadExpireSeconds", false, 900L),
                    fieldInt(resource, "accessTokenEnabled", false, DISABLED),
                    fieldInt(resource, "publicReadRequiresToken", false, DISABLED),
                    defaultText(fieldText(resource, "accessMode", false), "PROXY").toUpperCase(),
                    fieldLong(resource, "accessTokenExpireSeconds", false, 600L),
                    fieldText(resource, "previewProviderUrl", false),
                    fieldLong(resource, "previewExpireSeconds", false, 600L),
                    defaultText(fieldText(resource, "previewExternalExtensions", false),
                            "doc,docx,xls,xlsx,xlsm,ppt,pptx,odt,ods,odp,ofd,wps,et,dps,csv,txt,zip,rar,7z,eml,msg"),
                    fieldInt(resource, "archiveRetainEnabled", false, ENABLED),
                    fieldInt(resource, "archiveRetainDays", false, 180),
                    fieldInt(resource, "archiveRestoreEnabled", false, DISABLED),
                    fieldInt(resource, "physicalDeleteEnabled", false, DISABLED)
            );
        }
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("FILE_SETTINGS field is required: " + name);
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

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("FILE_SETTINGS long value is required");
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
                throw new IllegalStateException("FILE_SETTINGS int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
