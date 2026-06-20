package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.file.api.FileCode;
import io.mango.file.api.command.SaveFileSettingsCommand;
import io.mango.file.api.enums.FileAccessMode;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileDuplicateNameStrategy;
import io.mango.file.api.enums.FileInstantUploadScope;
import io.mango.file.api.enums.FileObjectNameStrategy;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileSettings;
import io.mango.file.core.mapper.FileSettingsMapper;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文件中心运行时配置服务实现。
 */
@Service
@RequiredArgsConstructor
public class FileSettingsServiceImpl implements IFileSettingsService {

    private final FileSettingsMapper mapper;
    private final FileProperties properties;
    private final Map<Long, FileSettingsVO> settingsCache = new ConcurrentHashMap<>();

    @Override
    public R<FileSettingsVO> get() {
        return R.ok(current());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> save(SaveFileSettingsCommand command) {
        Require.notNull(command, FileCode.STORAGE_SETTINGS_INVALID);
        Require.notNull(command.getMaxSize(), FileCode.STORAGE_SETTINGS_INVALID);
        Require.isTrue(command.getMaxSize() > 0, FileCode.STORAGE_SETTINGS_INVALID);
        validateExpire(command.getDirectUploadExpireSeconds(), "直传有效期必须大于0");
        validateExpire(command.getAccessTokenExpireSeconds(), "访问有效期必须大于0");
        validateExpire(command.getPreviewExpireSeconds(), "预览有效期必须大于0");
        if (command.getArchiveRetainDays() != null) {
            Require.isTrue(command.getArchiveRetainDays() > 0, FileCode.STORAGE_SETTINGS_INVALID);
        }

        Long tenantId = currentTenantId();
        FileSettings entity = selectByTenant(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            entity = new FileSettings();
            entity.setTenantId(tenantId);
            entity.setCreatedBy(MangoContextHolder.userId());
            entity.setCreatedTime(now);
        }
        copy(command, entity);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(now);
        persist(tenantId, entity);
        settingsCache.remove(tenantId);
        return R.ok(true);
    }

    @Override
    public FileSettingsVO current() {
        Long tenantId = currentTenantId();
        FileSettingsVO cached = settingsCache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        FileSettings entity = selectByTenant(tenantId);
        if (entity == null) {
            FileSettingsVO defaults = defaultVO(tenantId);
            settingsCache.put(tenantId, defaults);
            return defaults;
        }
        FileSettingsVO vo = toVO(entity);
        vo.setDefaultConfig(false);
        settingsCache.put(tenantId, vo);
        return vo;
    }

    private FileSettings selectByTenant(Long tenantId) {
        return mapper.selectOne(new LambdaQueryWrapper<FileSettings>()
                .eq(FileSettings::getTenantId, tenantId)
                .last("LIMIT 1"));
    }

    private void persist(Long tenantId, FileSettings entity) {
        if (entity.getId() != null) {
            mapper.updateById(entity);
            return;
        }
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException e) {
            FileSettings existing = selectByTenant(tenantId);
            Require.notNull(existing, FileCode.STORAGE_SETTINGS_SAVE_CONFLICT);
            entity.setId(existing.getId());
            entity.setCreatedBy(existing.getCreatedBy());
            entity.setCreatedTime(existing.getCreatedTime());
            mapper.updateById(entity);
        }
    }

    private void copy(SaveFileSettingsCommand command, FileSettings entity) {
        FileSettingsVO defaults = defaultVO();
        entity.setMaxSize(command.getMaxSize());
        entity.setAllowedExtensions(joinExtensions(command.getAllowedExtensions()));
        entity.setBlockedExtensions(joinExtensions(command.getBlockedExtensions()));
        entity.setDefaultAccessLevel(FileAccessLevel.of(command.getDefaultAccessLevel()).name());
        entity.setDuplicateNameStrategy(FileDuplicateNameStrategy.of(command.getDuplicateNameStrategy()).name());
        entity.setDuplicateCheckDirectoryScoped(Boolean.FALSE.equals(command.getDuplicateCheckDirectoryScoped()) ? 0 : 1);
        entity.setObjectNameStrategy(FileObjectNameStrategy.of(command.getObjectNameStrategy()).name());
        entity.setInstantUploadEnabled(Boolean.FALSE.equals(command.getInstantUploadEnabled()) ? 0 : 1);
        entity.setInstantUploadScope(FileInstantUploadScope.of(command.getInstantUploadScope()).name());
        entity.setContentTypeCheckEnabled(Boolean.FALSE.equals(command.getContentTypeCheckEnabled()) ? 0 : 1);
        entity.setAllowedContentTypes(joinTextValues(command.getAllowedContentTypes()));
        entity.setBlockedContentTypes(joinTextValues(command.getBlockedContentTypes()));
        entity.setDirectUploadEnabled(Boolean.TRUE.equals(command.getDirectUploadEnabled()) ? 1 : 0);
        entity.setDirectUploadExpireSeconds(command.getDirectUploadExpireSeconds() == null
                ? defaults.getDirectUploadExpireSeconds() : command.getDirectUploadExpireSeconds());
        entity.setAccessTokenEnabled(Boolean.TRUE.equals(command.getAccessTokenEnabled()) ? 1 : 0);
        entity.setPublicReadRequiresToken(Boolean.TRUE.equals(command.getPublicReadRequiresToken()) ? 1 : 0);
        entity.setAccessMode(FileAccessMode.of(command.getAccessMode()).name());
        entity.setAccessTokenExpireSeconds(command.getAccessTokenExpireSeconds() == null
                ? defaults.getAccessTokenExpireSeconds() : command.getAccessTokenExpireSeconds());
        entity.setPreviewProviderUrl(StringUtils.hasText(command.getPreviewProviderUrl())
                ? command.getPreviewProviderUrl().trim() : defaults.getPreviewProviderUrl());
        entity.setPreviewExpireSeconds(command.getPreviewExpireSeconds() == null
                ? defaults.getPreviewExpireSeconds() : command.getPreviewExpireSeconds());
        entity.setPreviewExternalExtensions(joinExtensions(command.getPreviewExternalExtensions()));
        entity.setArchiveRetainEnabled(Boolean.FALSE.equals(command.getArchiveRetainEnabled()) ? 0 : 1);
        entity.setArchiveRetainDays(command.getArchiveRetainDays() == null
                ? defaults.getArchiveRetainDays() : command.getArchiveRetainDays());
        entity.setArchiveRestoreEnabled(Boolean.TRUE.equals(command.getArchiveRestoreEnabled()) ? 1 : 0);
        entity.setPhysicalDeleteEnabled(Boolean.TRUE.equals(command.getPhysicalDeleteEnabled()) ? 1 : 0);
    }

    private FileSettingsVO toVO(FileSettings entity) {
        FileSettingsVO vo = new FileSettingsVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setMaxSize(entity.getMaxSize());
        vo.setAllowedExtensions(splitExtensions(entity.getAllowedExtensions()));
        vo.setBlockedExtensions(splitExtensions(entity.getBlockedExtensions()));
        vo.setDefaultAccessLevel(FileAccessLevel.of(entity.getDefaultAccessLevel()).name());
        vo.setDuplicateNameStrategy(FileDuplicateNameStrategy.of(entity.getDuplicateNameStrategy()).name());
        vo.setDuplicateCheckDirectoryScoped(!Integer.valueOf(0).equals(entity.getDuplicateCheckDirectoryScoped()));
        vo.setObjectNameStrategy(FileObjectNameStrategy.of(entity.getObjectNameStrategy()).name());
        vo.setInstantUploadEnabled(Integer.valueOf(1).equals(entity.getInstantUploadEnabled()));
        vo.setInstantUploadScope(FileInstantUploadScope.of(entity.getInstantUploadScope()).name());
        vo.setContentTypeCheckEnabled(!Integer.valueOf(0).equals(entity.getContentTypeCheckEnabled()));
        vo.setAllowedContentTypes(splitTextValues(entity.getAllowedContentTypes()));
        vo.setBlockedContentTypes(splitTextValues(entity.getBlockedContentTypes()));
        vo.setDirectUploadEnabled(Integer.valueOf(1).equals(entity.getDirectUploadEnabled()));
        vo.setDirectUploadExpireSeconds(entity.getDirectUploadExpireSeconds());
        vo.setAccessTokenEnabled(Integer.valueOf(1).equals(entity.getAccessTokenEnabled()));
        vo.setPublicReadRequiresToken(Integer.valueOf(1).equals(entity.getPublicReadRequiresToken()));
        vo.setAccessMode(FileAccessMode.of(entity.getAccessMode()).name());
        vo.setAccessTokenExpireSeconds(entity.getAccessTokenExpireSeconds());
        vo.setPreviewProviderUrl(StringUtils.hasText(entity.getPreviewProviderUrl())
                ? entity.getPreviewProviderUrl() : defaultVO(entity.getTenantId()).getPreviewProviderUrl());
        vo.setPreviewExpireSeconds(entity.getPreviewExpireSeconds());
        vo.setPreviewExternalExtensions(splitExtensions(entity.getPreviewExternalExtensions()));
        vo.setArchiveRetainEnabled(!Integer.valueOf(0).equals(entity.getArchiveRetainEnabled()));
        vo.setArchiveRetainDays(entity.getArchiveRetainDays());
        vo.setArchiveRestoreEnabled(Integer.valueOf(1).equals(entity.getArchiveRestoreEnabled()));
        vo.setPhysicalDeleteEnabled(Integer.valueOf(1).equals(entity.getPhysicalDeleteEnabled()));
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private FileSettingsVO defaultVO() {
        return defaultVO(currentTenantId());
    }

    private FileSettingsVO defaultVO(Long tenantId) {
        FileSettingsVO vo = new FileSettingsVO();
        vo.setTenantId(tenantId);
        vo.setMaxSize(properties.getUpload().getMaxSize());
        vo.setAllowedExtensions(normalizeExtensions(properties.getUpload().getAllowedExtensions()));
        vo.setBlockedExtensions(normalizeExtensions(properties.getUpload().getBlockedExtensions()));
        vo.setDefaultAccessLevel(FileAccessLevel.PRIVATE.name());
        vo.setDuplicateNameStrategy(FileDuplicateNameStrategy.REJECT.name());
        vo.setDuplicateCheckDirectoryScoped(true);
        vo.setObjectNameStrategy(FileObjectNameStrategy.DATE_UUID.name());
        vo.setInstantUploadEnabled(properties.getUpload().isInstantUploadEnabled());
        vo.setInstantUploadScope(FileInstantUploadScope.TENANT.name());
        vo.setContentTypeCheckEnabled(true);
        vo.setAllowedContentTypes(List.of());
        vo.setBlockedContentTypes(List.of("application/x-msdownload", "application/x-sh"));
        vo.setDirectUploadEnabled(properties.getUpload().isDirectUploadEnabled());
        vo.setDirectUploadExpireSeconds(properties.getUpload().getDirectUploadExpireSeconds());
        vo.setAccessTokenEnabled(properties.getAccess().isTokenEnabled());
        vo.setPublicReadRequiresToken(false);
        vo.setAccessMode(FileAccessMode.of(properties.getAccess().getMode()).name());
        vo.setAccessTokenExpireSeconds(properties.getAccess().getTokenExpireSeconds());
        vo.setPreviewProviderUrl(trimToNull(properties.getPreview().getProviderUrl()));
        vo.setPreviewExpireSeconds(properties.getPreview().getExpireSeconds());
        vo.setPreviewExternalExtensions(normalizeExtensions(properties.getPreview().getExternalExtensions()));
        vo.setArchiveRetainEnabled(true);
        vo.setArchiveRetainDays(180);
        vo.setArchiveRestoreEnabled(false);
        vo.setPhysicalDeleteEnabled(false);
        vo.setDefaultConfig(true);
        return vo;
    }

    private void validateExpire(Long value, String message) {
        if (value != null) {
            Require.isTrue(value > 0, FileCode.STORAGE_SETTINGS_INVALID);
        }
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return 1L;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            return 1L;
        }
    }

    private String joinExtensions(List<String> values) {
        List<String> normalized = normalizeExtensions(values);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private String joinTextValues(List<String> values) {
        List<String> normalized = normalizeTextValues(values);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private List<String> splitExtensions(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return normalizeExtensions(Arrays.asList(value.split(",")));
    }

    private List<String> splitTextValues(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return normalizeTextValues(Arrays.asList(value.split(",")));
    }

    private List<String> normalizeExtensions(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.trim().replace(".", "").toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeTextValues(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
