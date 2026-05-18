package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.FileCode;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileAccessMode;
import io.mango.file.api.enums.FileDuplicateNameStrategy;
import io.mango.file.api.enums.FileInstantUploadScope;
import io.mango.file.api.enums.FileObjectNameStrategy;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.entity.FileDirectory;
import io.mango.file.core.entity.FileRecord;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.file.core.mapper.FileDirectoryMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.service.FileDownload;
import io.mango.file.core.service.IFileDirectoryService;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.file.core.service.IFileStorageConfigService;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.infra.context.core.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 文件服务实现。
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int BIZ_META_MAX_LENGTH = 4000;

    private final FileStorageRouter fileStorageRouter;
    private final IFileStorageConfigService storageConfigService;
    private final IFileSettingsService settingsService;
    private final IFileDirectoryService directoryService;
    private final FileRecordMapper fileRecordMapper;
    private final FileDirectoryMapper fileDirectoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<FileRecordVO> upload(MultipartFile file,
                                  String purpose,
                                  String accessLevel,
                                  String bizType,
                                  String bizId,
                                  String bizMeta,
                                  Long directoryId) {
        Require.notNull(file, FileCode.FILE_EMPTY);
        FileInput input = FileInput.fromMultipart(file);
        return save(input, purpose, accessLevel, bizType, bizId, bizMeta, directoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<FileRecordVO> save(SaveFileCommand command) {
        Require.notNull(command, FileCode.FILE_EMPTY);
        try (FileInput input = FileInput.fromCommand(command)) {
            return save(input,
                    command.getPurpose(),
                    command.getAccessLevel(),
                    command.getBizType(),
                    command.getBizId(),
                    command.getBizMeta(),
                    command.getDirectoryId());
        } catch (IOException e) {
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    private R<FileRecordVO> save(FileInput input,
                                 String purpose,
                                 String accessLevel,
                                 String bizType,
                                 String bizId,
                                 String bizMeta,
                                 Long directoryId) {
        validateUpload(input);
        Long tenantId = requireTenantId();
        Long userId = MangoContextHolder.userId();
        Long resolvedDirectoryId = normalizeDirectoryId(directoryId);
        directoryService.selectVisible(resolvedDirectoryId);
        FileSettingsVO settings = settingsService.current();
        String originalFilename = resolveFileName(tenantId, resolvedDirectoryId, normalizeFileName(input.fileName), settings);
        String fileExt = fileExt(originalFilename);
        validateExtension(fileExt, settings);
        validateContentType(input.contentType, settings);
        String normalizedBizMeta = normalizeBizMeta(bizMeta);
        String hash = sha256(input);
        FileRecord reusedRecord = findInstantUploadRecord(tenantId, hash, settings);
        FileStorageConfig storageConfig;
        String objectName;
        if (reusedRecord != null) {
            storageConfig = storageConfigService.getEnabledConfig(
                    reusedRecord.getStorageConfigId(),
                    reusedRecord.getStorageType(),
                    reusedRecord.getBucketName());
            objectName = reusedRecord.getObjectName();
        } else {
            storageConfig = storageConfigService.activeConfig();
            Require.isTrue(Integer.valueOf(1).equals(storageConfig.getStatus()), FileCode.STORAGE_CONFIG_DISABLED);
            objectName = generateObjectName(storageConfig, tenantId, resolvedDirectoryId, originalFilename, fileExt, hash, settings);
            try (InputStream uploadInput = input.openStream()) {
                fileStorageRouter.putObject(storageConfig, objectName, uploadInput, input.fileSize, input.contentType);
            } catch (Exception e) {
                return Require.fail(FileCode.FILE_STORE_FAILED);
            }
        }

        FileRecord entity = new FileRecord();
        entity.setTenantId(tenantId);
        entity.setBizType(trimToNull(bizType));
        entity.setBizId(trimToNull(bizId));
        entity.setPurpose(trimToNull(purpose));
        entity.setBizMeta(normalizedBizMeta);
        entity.setDirectoryId(resolvedDirectoryId);
        entity.setAccessLevel(resolveAccessLevel(accessLevel, settings).name());
        entity.setStorageType(storageConfig.getStorageType());
        entity.setStorageConfigId(storageConfig.getId() == null || storageConfig.getId() <= 0 ? null : storageConfig.getId());
        entity.setBucketName(storageConfig.getBucketName());
        entity.setObjectName(objectName);
        entity.setFileName(originalFilename);
        entity.setFileExt(fileExt);
        entity.setFileSize(input.fileSize);
        entity.setContentType(trimToNull(input.contentType));
        entity.setFileHash(hash);
        entity.setStatus(FileRecordStatus.COMPLETED.value());
        entity.setArchived(0);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        fileRecordMapper.insert(entity);
        return R.ok(toVO(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<List<FileRecordVO>> uploadBatch(MultipartFile[] files,
                                             String purpose,
                                             String accessLevel,
                                             String bizType,
                                             String bizId,
                                             String bizMeta,
                                             Long directoryId) {
        Require.notEmpty(files == null ? List.of() : List.of(files), FileCode.FILE_EMPTY);
        List<FileRecordVO> result = List.of(files).stream()
                .filter(item -> item != null && !item.isEmpty())
                .map(item -> upload(item, purpose, accessLevel, bizType, bizId, bizMeta, directoryId).getData())
                .collect(Collectors.toList());
        return R.ok(result);
    }

    @Override
    public R<PageResult<FileRecordVO>> page(FileRecordPageQuery query) {
        FileRecordPageQuery resolved = query == null ? new FileRecordPageQuery() : query;
        IPage<FileRecord> page = fileRecordMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<FileRecordVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<FileRecordVO> get(Long id) {
        return R.ok(toVO(selectVisible(id)));
    }

    @Override
    public R<FilePreviewVO> preview(Long id) {
        FileRecord record = selectVisible(id);
        FileSettingsVO settings = settingsService.current();
        FilePreviewVO vo = new FilePreviewVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setFileExt(record.getFileExt());
        vo.setFileSize(record.getFileSize());
        vo.setContentType(record.getContentType());
        vo.setPreviewable(isPreviewable(record));
        fillDirectAccess(vo, record, settings);
        return R.ok(vo);
    }

    @Override
    public FileDownload download(Long id) {
        FileRecord record = selectVisible(id);
        FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                record.getStorageConfigId(),
                record.getStorageType(),
                record.getBucketName());
        FileObject object = fileStorageRouter.getObject(storageConfig, record.getObjectName());
        String contentType = StringUtils.hasText(record.getContentType()) ? record.getContentType() : object.contentType();
        return new FileDownload(object.inputStream(), record.getFileName(), contentType, object.contentLength());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> archive(FileArchiveCommand command) {
        Require.notNull(command, FileCode.FILE_STATUS_INVALID);
        FileRecord record = selectVisible(command.getId());
        record.setStatus(FileRecordStatus.ARCHIVED.value());
        record.setArchived(1);
        record.setUpdatedBy(MangoContextHolder.userId());
        record.setUpdatedTime(LocalDateTime.now());
        boolean updated = fileRecordMapper.updateById(record) > 0;
        if (updated && Boolean.TRUE.equals(settingsService.current().getPhysicalDeleteEnabled())) {
            removePhysicalObjectIfUnreferenced(record);
        }
        return R.ok(updated);
    }

    private LambdaQueryWrapper<FileRecord> wrapper(FileRecordPageQuery query) {
        LambdaQueryWrapper<FileRecord> wrapper = tenantVisibleWrapper();
        String keyword = trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), nested -> nested
                .like(FileRecord::getFileName, keyword)
                .or()
                .like(FileRecord::getBizType, keyword)
                .or()
                .like(FileRecord::getBizId, keyword));
        wrapper.eq(StringUtils.hasText(query.getBizType()), FileRecord::getBizType, query.getBizType());
        wrapper.eq(StringUtils.hasText(query.getBizId()), FileRecord::getBizId, query.getBizId());
        wrapper.eq(StringUtils.hasText(query.getPurpose()), FileRecord::getPurpose, query.getPurpose());
        wrapper.eq(query.getDirectoryId() != null, FileRecord::getDirectoryId, normalizeDirectoryId(query.getDirectoryId()));
        wrapper.eq(StringUtils.hasText(query.getAccessLevel()), FileRecord::getAccessLevel, query.getAccessLevel());
        wrapper.eq(query.getStatus() != null, FileRecord::getStatus, query.getStatus());
        if (!Boolean.TRUE.equals(query.getIncludeArchived())) {
            wrapper.eq(FileRecord::getArchived, 0);
        }
        wrapper.orderByDesc(FileRecord::getCreatedTime);
        return wrapper;
    }

    private LambdaQueryWrapper<FileRecord> tenantVisibleWrapper() {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileRecord::getTenantId, requireTenantId());
        return wrapper;
    }

    private FileRecord selectVisible(Long id) {
        Require.notNull(id, FileCode.FILE_NOT_FOUND);
        FileRecord record = fileRecordMapper.selectOne(tenantVisibleWrapper().eq(FileRecord::getId, id).last("LIMIT 1"));
        Require.notNull(record, FileCode.FILE_NOT_FOUND);
        Require.isFalse(FileRecordStatus.ARCHIVED.value() == record.getStatus()
                || Integer.valueOf(1).equals(record.getArchived()), FileCode.FILE_NOT_FOUND);
        return record;
    }

    private void validateUpload(FileInput input) {
        Require.notNull(input, FileCode.FILE_EMPTY);
        Require.isTrue(input != null && input.streamSupplier != null, FileCode.FILE_EMPTY);
        Require.isFalse(input.empty, FileCode.FILE_EMPTY);
        long maxSize = settingsService.current().getMaxSize();
        Require.isTrue(input.fileSize <= maxSize, FileCode.FILE_SIZE_EXCEEDED);
    }

    private void validateExtension(String fileExt, FileSettingsVO settings) {
        String ext = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
        List<String> blocked = settings.getBlockedExtensions();
        Require.isFalse(blocked != null && blocked.stream().map(item -> item.toLowerCase(Locale.ROOT)).anyMatch(ext::equals),
                FileCode.FILE_EXTENSION_BLOCKED);
        List<String> allowed = settings.getAllowedExtensions();
        Require.isTrue(allowed == null || allowed.isEmpty()
                        || allowed.stream().map(item -> item.toLowerCase(Locale.ROOT)).anyMatch(ext::equals),
                FileCode.FILE_EXTENSION_NOT_ALLOWED);
    }

    private void validateContentType(String contentType, FileSettingsVO settings) {
        if (!Boolean.TRUE.equals(settings.getContentTypeCheckEnabled())) {
            return;
        }
        String type = StringUtils.hasText(contentType) ? contentType.toLowerCase(Locale.ROOT) : "";
        List<String> blocked = settings.getBlockedContentTypes();
        Require.isFalse(StringUtils.hasText(type) && blocked != null
                        && blocked.stream().map(item -> item.toLowerCase(Locale.ROOT)).anyMatch(type::equals),
                FileCode.FILE_EXTENSION_BLOCKED);
        List<String> allowed = settings.getAllowedContentTypes();
        Require.isTrue(!StringUtils.hasText(type) || allowed == null || allowed.isEmpty()
                        || allowed.stream().map(item -> item.toLowerCase(Locale.ROOT)).anyMatch(type::equals),
                FileCode.FILE_EXTENSION_NOT_ALLOWED);
    }

    private Long requireTenantId() {
        Long tenantId = currentTenantId();
        Require.notNull(tenantId, FileCode.FILE_ACCESS_DENIED);
        return tenantId;
    }

    private Long currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isPreviewable(FileRecord record) {
        String contentType = record.getContentType();
        if (StringUtils.hasText(contentType) && (contentType.startsWith("image/")
                || contentType.startsWith("text/")
                || contentType.startsWith("video/")
                || contentType.startsWith("audio/")
                || "application/pdf".equalsIgnoreCase(contentType))) {
            return true;
        }
        String ext = record.getFileExt();
        if (!StringUtils.hasText(ext)) {
            return false;
        }
        return settingsService.current().getPreviewExternalExtensions().stream()
                .anyMatch(item -> item.equalsIgnoreCase(ext));
    }

    private void fillDirectAccess(FilePreviewVO vo, FileRecord record, FileSettingsVO settings) {
        vo.setDirectAccess(false);
        vo.setPreviewUrl(relativeDownloadUrl(record.getId()));
        vo.setDownloadUrl(relativeDownloadUrl(record.getId()));
        if (FileAccessMode.of(settings.getAccessMode()) != FileAccessMode.DIRECT) {
            return;
        }
        FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                record.getStorageConfigId(),
                record.getStorageType(),
                record.getBucketName());
        if (!shouldUsePresignedUrl(record, settings)) {
            fileStorageRouter.publicGetUrl(storageConfig, record.getObjectName(), record.getFileName())
                    .ifPresent(url -> {
                        vo.setDirectAccess(true);
                        vo.setDirectPreviewUrl(url);
                    });
            fileStorageRouter.publicDownloadUrl(storageConfig, record.getObjectName(), record.getFileName())
                    .ifPresent(url -> {
                        vo.setDirectAccess(true);
                        vo.setDirectDownloadUrl(url);
                    });
            return;
        }
        long previewExpireSeconds = positiveOrDefault(settings.getPreviewExpireSeconds(), 600L);
        long downloadExpireSeconds = positiveOrDefault(settings.getAccessTokenExpireSeconds(), 600L);
        fileStorageRouter.presignedGetUrl(storageConfig, record.getObjectName(), record.getFileName(),
                        Duration.ofSeconds(previewExpireSeconds))
                .ifPresent(url -> {
                    vo.setDirectAccess(true);
                    vo.setDirectPreviewUrl(url);
                    vo.setDirectPreviewExpireSeconds(previewExpireSeconds);
                });
        fileStorageRouter.presignedDownloadUrl(storageConfig, record.getObjectName(), record.getFileName(),
                        Duration.ofSeconds(downloadExpireSeconds))
                .ifPresent(url -> {
                    vo.setDirectAccess(true);
                    vo.setDirectDownloadUrl(url);
                    vo.setDirectDownloadExpireSeconds(downloadExpireSeconds);
                });
    }

    private void fillDirectAccess(FileRecordVO vo, FileRecord record, FileSettingsVO settings) {
        String fallbackUrl = relativeDownloadUrl(record.getId());
        vo.setUrl(fallbackUrl);
        vo.setDownloadUrl(fallbackUrl);
        vo.setDirectAccess(false);
        FileStorageConfig storageConfig = resolveStorageConfig(record);
        boolean directMode = FileAccessMode.of(settings.getAccessMode()) == FileAccessMode.DIRECT;
        if (!directMode || !shouldUsePresignedUrl(record, settings)) {
            fileStorageRouter.publicGetUrl(storageConfig, record.getObjectName(), record.getFileName())
                    .ifPresent(url -> {
                        vo.setDirectAccess(true);
                        vo.setUrl(url);
                        vo.setDirectPreviewUrl(url);
                    });
            fileStorageRouter.publicDownloadUrl(storageConfig, record.getObjectName(), record.getFileName())
                    .ifPresent(url -> {
                        vo.setDirectAccess(true);
                        vo.setDownloadUrl(url);
                        vo.setDirectDownloadUrl(url);
                    });
            return;
        }
        long previewExpireSeconds = positiveOrDefault(settings.getPreviewExpireSeconds(), 600L);
        long downloadExpireSeconds = positiveOrDefault(settings.getAccessTokenExpireSeconds(), 600L);
        fileStorageRouter.presignedGetUrl(storageConfig, record.getObjectName(), record.getFileName(),
                        Duration.ofSeconds(previewExpireSeconds))
                .ifPresent(url -> {
                    vo.setDirectAccess(true);
                    vo.setUrl(url);
                    vo.setDirectPreviewUrl(url);
                    vo.setDirectPreviewExpireSeconds(previewExpireSeconds);
                });
        fileStorageRouter.presignedDownloadUrl(storageConfig, record.getObjectName(), record.getFileName(),
                        Duration.ofSeconds(downloadExpireSeconds))
                .ifPresent(url -> {
                    vo.setDirectAccess(true);
                    vo.setDownloadUrl(url);
                    vo.setDirectDownloadUrl(url);
                    vo.setDirectDownloadExpireSeconds(downloadExpireSeconds);
                });
    }

    private FileStorageConfig resolveStorageConfig(FileRecord record) {
        return storageConfigService.getEnabledConfig(
                record.getStorageConfigId(),
                record.getStorageType(),
                record.getBucketName());
    }

    private String relativeDownloadUrl(Long fileId) {
        return "/file/files/download?id=" + fileId;
    }

    private boolean shouldUsePresignedUrl(FileRecord record, FileSettingsVO settings) {
        if (Boolean.TRUE.equals(settings.getAccessTokenEnabled())) {
            return true;
        }
        FileAccessLevel accessLevel = FileAccessLevel.of(record.getAccessLevel());
        return accessLevel == FileAccessLevel.PRIVATE
                || accessLevel == FileAccessLevel.INTERNAL
                || Boolean.TRUE.equals(settings.getPublicReadRequiresToken());
    }

    private long positiveOrDefault(Long value, long defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private void requireUniqueFileName(Long tenantId, Long directoryId, String fileName) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getTenantId, tenantId)
                .eq(FileRecord::getFileName, fileName)
                .eq(FileRecord::getArchived, 0);
        wrapper.eq(directoryId != null, FileRecord::getDirectoryId, directoryId);
        Long count = fileRecordMapper.selectCount(wrapper);
        Require.isTrue(count == 0, FileCode.FILE_NAME_DUPLICATED);
    }

    private String resolveFileName(Long tenantId, Long directoryId, String fileName, FileSettingsVO settings) {
        FileDuplicateNameStrategy strategy = FileDuplicateNameStrategy.of(settings.getDuplicateNameStrategy());
        if (strategy == FileDuplicateNameStrategy.ALLOW) {
            return fileName;
        }
        if (strategy == FileDuplicateNameStrategy.REJECT) {
            requireUniqueFileName(tenantId, duplicateDirectoryId(directoryId, settings), fileName);
            return fileName;
        }
        return autoRenameFileName(tenantId, duplicateDirectoryId(directoryId, settings), fileName);
    }

    private String autoRenameFileName(Long tenantId, Long directoryId, String fileName) {
        if (!fileNameExists(tenantId, directoryId, fileName)) {
            return fileName;
        }
        String base = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        for (int index = 1; index <= 999; index++) {
            String candidate = base + "(" + index + ")" + ext;
            if (!fileNameExists(tenantId, directoryId, candidate)) {
                return candidate;
            }
        }
        return Require.fail(FileCode.FILE_NAME_DUPLICATED);
    }

    private boolean fileNameExists(Long tenantId, Long directoryId, String fileName) {
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getTenantId, tenantId)
                .eq(FileRecord::getFileName, fileName)
                .eq(FileRecord::getArchived, 0);
        wrapper.eq(directoryId != null, FileRecord::getDirectoryId, directoryId);
        return fileRecordMapper.selectCount(wrapper) > 0;
    }

    private Long duplicateDirectoryId(Long directoryId, FileSettingsVO settings) {
        return Boolean.FALSE.equals(settings.getDuplicateCheckDirectoryScoped()) ? null : directoryId;
    }

    private FileAccessLevel resolveAccessLevel(String accessLevel, FileSettingsVO settings) {
        if (StringUtils.hasText(accessLevel)) {
            return FileAccessLevel.of(accessLevel);
        }
        return FileAccessLevel.of(settings.getDefaultAccessLevel());
    }

    private Long normalizeDirectoryId(Long value) {
        return value == null || value < 0 ? 0L : value;
    }

    private String generateObjectName(FileStorageConfig storageConfig,
                                      Long tenantId,
                                      Long directoryId,
                                      String fileName,
                                      String fileExt,
                                      String hash,
                                      FileSettingsVO settings) {
        FileObjectNameStrategy strategy = FileObjectNameStrategy.of(settings.getObjectNameStrategy());
        String datePath = LocalDateTime.now().format(DATE_PATH);
        String id = UUID.randomUUID().toString().replace("-", "");
        String suffix = StringUtils.hasText(fileExt) ? "." + fileExt : "";
        String relative;
        if (strategy == FileObjectNameStrategy.HASH && StringUtils.hasText(hash)) {
            relative = "tenant-" + tenantId + "/sha256/" + hash.substring(0, 2) + "/" + hash.substring(2, 4) + "/" + hash + suffix;
        } else if (strategy == FileObjectNameStrategy.ORIGINAL) {
            relative = "tenant-" + tenantId + "/" + datePath + "/dir-" + normalizeDirectoryId(directoryId) + "/" + id + "-" + normalizeFileName(fileName);
        } else {
            relative = "tenant-" + tenantId + "/" + datePath + "/" + id + suffix;
        }
        String storagePath = normalizeObjectPrefix(storageConfig.getStoragePath());
        return StringUtils.hasText(storagePath) ? storagePath + "/" + relative : relative;
    }

    private String sha256(FileInput file) {
        try (InputStream input = file.openStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    private FileRecord findInstantUploadRecord(Long tenantId, String hash, FileSettingsVO settings) {
        if (!Boolean.TRUE.equals(settings.getInstantUploadEnabled()) || !StringUtils.hasText(hash)) {
            return null;
        }
        LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getFileHash, hash)
                .eq(FileRecord::getStatus, FileRecordStatus.COMPLETED.value())
                .eq(FileRecord::getArchived, 0)
                .orderByDesc(FileRecord::getCreatedTime)
                .last("LIMIT 1");
        if (FileInstantUploadScope.of(settings.getInstantUploadScope()) == FileInstantUploadScope.TENANT) {
            wrapper.eq(FileRecord::getTenantId, tenantId);
        }
        return fileRecordMapper.selectOne(wrapper);
    }

    private void removePhysicalObjectIfUnreferenced(FileRecord record) {
        Long activeRefs = fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getStorageType, record.getStorageType())
                .eq(FileRecord::getBucketName, record.getBucketName())
                .eq(FileRecord::getObjectName, record.getObjectName())
                .eq(record.getStorageConfigId() != null, FileRecord::getStorageConfigId, record.getStorageConfigId())
                .eq(FileRecord::getArchived, 0)
                .eq(FileRecord::getStatus, FileRecordStatus.COMPLETED.value()));
        if (activeRefs > 0) {
            return;
        }
        FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                record.getStorageConfigId(),
                record.getStorageType(),
                record.getBucketName());
        try {
            fileStorageRouter.removeObject(storageConfig, record.getObjectName());
        } catch (Exception e) {
            Require.fail(FileCode.FILE_READ_FAILED);
        }
    }

    private String normalizeObjectPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String path = value.trim().replace("\\", "/");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        Require.isFalse(path.contains(".."), FileCode.STORAGE_PATH_INVALID);
        return path;
    }

    private String normalizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "unnamed";
        }
        return PathSanitizer.fileName(fileName.trim());
    }

    private String fileExt(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeBizMeta(String value) {
        String bizMeta = trimToNull(value);
        if (bizMeta == null) {
            return null;
        }
        Require.isTrue(bizMeta.length() <= BIZ_META_MAX_LENGTH, FileCode.STORAGE_SETTINGS_INVALID);
        try {
            objectMapper.readTree(bizMeta);
            return bizMeta;
        } catch (Exception e) {
            return Require.fail(FileCode.STORAGE_SETTINGS_INVALID);
        }
    }

    private FileRecordVO toVO(FileRecord entity) {
        FileRecordVO vo = new FileRecordVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setPurpose(entity.getPurpose());
        vo.setBizMeta(entity.getBizMeta());
        vo.setDirectoryId(entity.getDirectoryId());
        vo.setDirectoryName(directoryName(entity.getDirectoryId()));
        vo.setAccessLevel(entity.getAccessLevel());
        vo.setStorageType(entity.getStorageType());
        vo.setStorageConfigId(entity.getStorageConfigId());
        vo.setBucketName(entity.getBucketName());
        vo.setObjectName(entity.getObjectName());
        vo.setFileName(entity.getFileName());
        vo.setFileExt(entity.getFileExt());
        vo.setFileSize(entity.getFileSize());
        vo.setContentType(entity.getContentType());
        vo.setFileHash(entity.getFileHash());
        vo.setStatus(entity.getStatus());
        vo.setArchived(entity.getArchived());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        fillDirectAccess(vo, entity, settingsService.current());
        return vo;
    }

    private static final class FileInput implements AutoCloseable {

        private final Supplier<InputStream> streamSupplier;
        private final Path tempPath;
        private final String fileName;
        private final long fileSize;
        private final String contentType;
        private final boolean empty;

        private FileInput(Supplier<InputStream> streamSupplier,
                          Path tempPath,
                          String fileName,
                          long fileSize,
                          String contentType,
                          boolean empty) {
            this.streamSupplier = streamSupplier;
            this.tempPath = tempPath;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.empty = empty;
        }

        private static FileInput fromMultipart(MultipartFile file) {
            return new FileInput(() -> {
                try {
                    return file.getInputStream();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }, null, file.getOriginalFilename(), file.getSize(), file.getContentType(), file.isEmpty());
        }

        private static FileInput fromCommand(SaveFileCommand command) throws IOException {
            Require.notNull(command.getInputStream(), FileCode.FILE_EMPTY);
            Path temp = Files.createTempFile("mango-file-", ".upload");
            long size;
            try (InputStream input = command.getInputStream(); OutputStream output = Files.newOutputStream(temp)) {
                size = input.transferTo(output);
            }
            long declaredSize = command.getFileSize() == null || command.getFileSize() <= 0 ? size : command.getFileSize();
            return new FileInput(() -> {
                try {
                    return Files.newInputStream(temp);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }, temp, command.getFileName(), declaredSize, command.getContentType(), declaredSize <= 0);
        }

        private InputStream openStream() {
            return streamSupplier.get();
        }

        @Override
        public void close() throws IOException {
            if (tempPath != null) {
                Files.deleteIfExists(tempPath);
            }
        }
    }

    private String directoryName(Long directoryId) {
        if (directoryId == null || directoryId <= 0) {
            return "根目录";
        }
        FileDirectory directory = fileDirectoryMapper.selectById(directoryId);
        return directory == null ? "" : directory.getDirectoryName();
    }

    /**
     * 文件名清理，避免客户端文件名携带路径。
     */
    private static final class PathSanitizer {

        private PathSanitizer() {
        }

        private static String fileName(String value) {
            String normalized = value.replace("\\", "/");
            int slash = normalized.lastIndexOf('/');
            return slash >= 0 ? normalized.substring(slash + 1) : normalized;
        }
    }
}
