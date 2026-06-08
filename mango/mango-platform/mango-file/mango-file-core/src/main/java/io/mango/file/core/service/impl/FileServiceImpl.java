package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileCode;
import io.mango.file.api.command.CompleteFileUploadPartCommand;
import io.mango.file.api.command.CreateFileUploadPartSignCommand;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileAccessMode;
import io.mango.file.api.enums.FileDuplicateNameStrategy;
import io.mango.file.api.enums.FileInstantUploadScope;
import io.mango.file.api.enums.FileObjectStatus;
import io.mango.file.api.enums.FileObjectNameStrategy;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.enums.FileUploadMode;
import io.mango.file.api.enums.FileUploadSessionStatus;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.api.vo.FileUploadInitVO;
import io.mango.file.api.vo.FileUploadPartSignVO;
import io.mango.file.core.entity.FileDirectory;
import io.mango.file.core.entity.FileHashMappingEntity;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecord;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.file.core.entity.FileUploadPartEntity;
import io.mango.file.core.entity.FileUploadSessionEntity;
import io.mango.file.core.mapper.FileDirectoryMapper;
import io.mango.file.core.mapper.FileHashMappingMapper;
import io.mango.file.core.mapper.FileObjectMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.mapper.FileUploadPartMapper;
import io.mango.file.core.mapper.FileUploadSessionMapper;
import io.mango.file.core.service.IFileDirectoryService;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.file.core.service.IFileStorageConfigService;
import io.mango.file.core.storage.CompletedUploadPart;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.file.core.storage.MultipartUpload;
import io.mango.file.core.storage.UploadPartSign;
import io.mango.infra.context.core.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
    private static final long DEFAULT_CHUNK_SIZE = 10L * 1024 * 1024;
    private static final long MIN_CHUNK_SIZE = 5L * 1024 * 1024;
    private static final long UPLOAD_SESSION_EXPIRE_HOURS = 24L;
    private static final String PART_STATUS_COMPLETED = "COMPLETED";

    private final FileStorageRouter fileStorageRouter;
    private final IFileStorageConfigService storageConfigService;
    private final IFileSettingsService settingsService;
    private final IFileDirectoryService directoryService;
    private final FileRecordMapper fileRecordMapper;
    private final FileObjectMapper fileObjectMapper;
    private final FileHashMappingMapper fileHashMappingMapper;
    private final FileUploadSessionMapper fileUploadSessionMapper;
    private final FileUploadPartMapper fileUploadPartMapper;
    private final FileDirectoryMapper fileDirectoryMapper;
    private final ObjectMapper objectMapper;
    private final FileAccessUrlAssembler fileAccessUrlAssembler;

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
        FileStorageConfig storageConfig = activeStorageConfig();
        FileObjectEntity fileObject = findInstantUploadObject(tenantId, storageConfig, hash, input.fileSize, settings);
        if (fileObject == null) {
            String objectName = generateObjectName(storageConfig, tenantId, resolvedDirectoryId, originalFilename, fileExt, hash, settings);
            try (InputStream uploadInput = input.openStream()) {
                fileStorageRouter.putObject(storageConfig, objectName, uploadInput, input.fileSize, input.contentType);
            } catch (Exception e) {
                return Require.fail(FileCode.FILE_STORE_FAILED);
            }
            fileObject = createFileObject(tenantId, storageConfig, objectName, hash, input.fileSize, input.contentType, 0L);
            createHashMapping(tenantId, storageConfig, hash, input.fileSize, fileObject, settings);
        }

        FileRecord entity = createFileRecord(tenantId,
                userId,
                fileObject,
                originalFilename,
                fileExt,
                input.fileSize,
                input.contentType,
                hash,
                purpose,
                accessLevel,
                bizType,
                bizId,
                normalizedBizMeta,
                resolvedDirectoryId,
                settings);
        fileRecordMapper.insert(entity);
        incrementObjectRefCount(fileObject.getId());
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
    @Transactional(rollbackFor = Exception.class)
    public R<FileRecordVO> saveGenerated(byte[] content,
                                         String fileName,
                                         String contentType,
                                         String purpose,
                                         String bizType,
                                         String bizId) {
        Require.notNull(content, FileCode.FILE_EMPTY);
        Long tenantId = requireTenantId();
        Long userId = MangoContextHolder.userId();
        String originalFilename = normalizeFileName(fileName);
        String fileExt = fileExt(originalFilename);
        FileSettingsVO settings = settingsService.current();
        validateExtension(fileExt, settings);
        validateContentType(contentType, settings);
        FileStorageConfig storageConfig = activeStorageConfig();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(digest.digest(content));
            Long fileSize = (long) content.length;
            FileObjectEntity fileObject = findInstantUploadObject(tenantId, storageConfig, hash, fileSize, settings);
            if (fileObject == null) {
                fileObject = findCompletedObject(storageConfig, hash, fileSize);
            }
            if (fileObject == null) {
                String objectName = generateObjectName(storageConfig, tenantId, 0L, originalFilename, fileExt, hash, settings);
                fileStorageRouter.putObject(storageConfig, objectName, new ByteArrayInputStream(content), content.length, contentType);
                fileObject = createFileObject(tenantId, storageConfig, objectName, hash, fileSize, contentType, 0L);
                createHashMapping(tenantId, storageConfig, hash, fileSize, fileObject, settings);
            }
            FileRecord entity = createFileRecord(tenantId,
                    userId,
                    fileObject,
                    originalFilename,
                    fileExt,
                    fileSize,
                    contentType,
                    hash,
                    purpose,
                    FileAccessLevel.PRIVATE.name(),
                    bizType,
                    bizId,
                    null,
                    0L,
                    settings);
            fileRecordMapper.insert(entity);
            incrementObjectRefCount(fileObject.getId());
            return R.ok(toVO(entity));
        } catch (Exception e) {
            return Require.fail(FileCode.FILE_STORE_FAILED);
        }
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
    public FileDownloadVO download(Long id) {
        return downloadForService(id);
    }

    @Override
    public FileDownloadVO downloadForService(Long id) {
        FileRecord record = selectVisible(id);
        StoredObject storedObject = resolveStoredObject(record);
        FileObject object = fileStorageRouter.getObject(storedObject.storageConfig(), storedObject.objectName());
        String contentType = StringUtils.hasText(record.getContentType()) ? record.getContentType() : object.contentType();
        return new FileDownloadVO(object.inputStream(), record.getFileName(), contentType, object.contentLength());
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
        if (updated && record.getObjectId() != null) {
            decrementObjectRefCount(record.getObjectId());
        }
        if (updated && Boolean.TRUE.equals(settingsService.current().getPhysicalDeleteEnabled())) {
            removePhysicalObjectIfUnreferenced(record);
        }
        return R.ok(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(FileDeleteCommand command) {
        Require.notNull(command, FileCode.FILE_STATUS_INVALID);
        Require.notEmpty(command.getIds(), FileCode.FILE_NOT_FOUND);
        List<Long> ids = command.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Require.notEmpty(ids, FileCode.FILE_NOT_FOUND);
        List<FileRecord> records = selectVisible(ids);
        Require.isTrue(records.size() == ids.size(), FileCode.FILE_NOT_FOUND);
        LocalDateTime now = LocalDateTime.now();
        Long userId = MangoContextHolder.userId();
        for (FileRecord record : records) {
            record.setStatus(FileRecordStatus.DELETED.value());
            record.setArchived(1);
            record.setUpdatedBy(userId);
            record.setUpdatedTime(now);
            fileRecordMapper.updateById(record);
            if (record.getObjectId() != null) {
                decrementObjectRefCount(record.getObjectId());
            }
        }
        if (Boolean.TRUE.equals(settingsService.current().getPhysicalDeleteEnabled())) {
            records.forEach(this::removePhysicalObjectIfUnreferenced);
        }
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<FileUploadInitVO> createUploadSession(CreateFileUploadSessionCommand command) {
        Require.notNull(command, FileCode.FILE_EMPTY);
        Long tenantId = requireTenantId();
        Long userId = MangoContextHolder.userId();
        Long resolvedDirectoryId = normalizeDirectoryId(command.getDirectoryId());
        directoryService.selectVisible(resolvedDirectoryId);
        FileSettingsVO settings = settingsService.current();
        Require.isTrue(command.getFileSize() <= settings.getMaxSize(), FileCode.FILE_SIZE_EXCEEDED);
        String fileName = resolveFileName(tenantId, resolvedDirectoryId, normalizeFileName(command.getFileName()), settings);
        String fileExt = fileExt(fileName);
        validateExtension(fileExt, settings);
        validateContentType(command.getContentType(), settings);
        String fileHash = trimToNull(command.getFileHash());
        Require.notBlank(fileHash, FileCode.FILE_UPLOAD_PART_INVALID);
        String normalizedBizMeta = normalizeBizMeta(command.getBizMeta());
        FileStorageConfig storageConfig = activeStorageConfig();
        FileObjectEntity instantObject = findInstantUploadObject(tenantId, storageConfig, fileHash, command.getFileSize(), settings);
        if (instantObject != null) {
            FileRecord record = createFileRecord(tenantId,
                    userId,
                    instantObject,
                    fileName,
                    fileExt,
                    command.getFileSize(),
                    command.getContentType(),
                    fileHash,
                    command.getPurpose(),
                    command.getAccessLevel(),
                    command.getBizType(),
                    command.getBizId(),
                    normalizedBizMeta,
                    resolvedDirectoryId,
                    settings);
            fileRecordMapper.insert(record);
            incrementObjectRefCount(instantObject.getId());
            FileUploadInitVO vo = new FileUploadInitVO();
            vo.setInstant(true);
            vo.setFileRecord(toVO(record));
            return R.ok(vo);
        }

        long chunkSize = resolveChunkSize(command.getChunkSize());
        int totalParts = resolveTotalParts(command.getTotalParts(), command.getFileSize(), chunkSize);
        String objectName = generateObjectName(storageConfig, tenantId, resolvedDirectoryId, fileName, fileExt, fileHash, settings);
        FileUploadMode uploadMode = fileStorageRouter.supportsMultipartUpload(storageConfig)
                ? FileUploadMode.S3_MULTIPART : FileUploadMode.SERVER_CHUNK;
        String storageUploadId = null;
        if (uploadMode == FileUploadMode.S3_MULTIPART) {
            MultipartUpload upload = fileStorageRouter.initiateMultipartUpload(storageConfig, objectName, command.getContentType());
            storageUploadId = upload.uploadId();
        }
        FileUploadSessionEntity session = new FileUploadSessionEntity();
        session.setTenantId(tenantId);
        session.setStorageConfigId(normalizedStorageConfigId(storageConfig));
        session.setStorageType(storageConfig.getStorageType());
        session.setBucketName(storageConfig.getBucketName());
        session.setObjectName(objectName);
        session.setUploadMode(uploadMode.name());
        session.setStorageUploadId(storageUploadId);
        session.setFileName(fileName);
        session.setFileExt(fileExt);
        session.setFileHash(fileHash);
        session.setFileSize(command.getFileSize());
        session.setContentType(trimToNull(command.getContentType()));
        session.setChunkSize(chunkSize);
        session.setTotalParts(totalParts);
        session.setUploadedParts(0);
        session.setStatus(FileUploadSessionStatus.INIT.name());
        session.setExpiresAt(LocalDateTime.now().plusHours(UPLOAD_SESSION_EXPIRE_HOURS));
        session.setPurpose(trimToNull(command.getPurpose()));
        session.setAccessLevel(resolveAccessLevel(command.getAccessLevel(), settings).name());
        session.setBizType(trimToNull(command.getBizType()));
        session.setBizId(trimToNull(command.getBizId()));
        session.setBizMeta(normalizedBizMeta);
        session.setDirectoryId(resolvedDirectoryId);
        session.setCreatedBy(userId);
        session.setUpdatedBy(userId);
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedTime(now);
        session.setUpdatedTime(now);
        fileUploadSessionMapper.insert(session);

        FileUploadInitVO vo = new FileUploadInitVO();
        vo.setInstant(false);
        vo.setSessionId(session.getId());
        vo.setUploadMode(session.getUploadMode());
        vo.setStorageUploadId(session.getStorageUploadId());
        vo.setChunkSize(session.getChunkSize());
        vo.setTotalParts(session.getTotalParts());
        vo.setExpiresAt(session.getExpiresAt());
        return R.ok(vo);
    }

    @Override
    public R<FileUploadPartSignVO> createUploadPartSign(Long sessionId, CreateFileUploadPartSignCommand command) {
        Require.notNull(command, FileCode.FILE_UPLOAD_PART_INVALID);
        FileUploadSessionEntity session = selectUploadSession(sessionId);
        requireUploadSessionActive(session);
        Require.isTrue(command.getPartNumber() <= session.getTotalParts(), FileCode.FILE_UPLOAD_PART_INVALID);
        Require.isTrue(FileUploadMode.S3_MULTIPART.name().equals(session.getUploadMode()), FileCode.FILE_UPLOAD_SESSION_INVALID);
        FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                session.getStorageConfigId(),
                session.getStorageType(),
                session.getBucketName());
        long expireSeconds = positiveOrDefault(settingsService.current().getDirectUploadExpireSeconds(), 900L);
        UploadPartSign sign = fileStorageRouter.presignedUploadPartUrl(storageConfig,
                session.getObjectName(),
                session.getStorageUploadId(),
                command.getPartNumber(),
                Duration.ofSeconds(expireSeconds));
        FileUploadPartSignVO vo = new FileUploadPartSignVO();
        vo.setPartNumber(sign.partNumber());
        vo.setUploadUrl(sign.uploadUrl());
        vo.setMethod(sign.method());
        vo.setExpireSeconds(sign.expireSeconds());
        return R.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> uploadServerPart(Long sessionId, Integer partNumber, MultipartFile file) {
        Require.notNull(file, FileCode.FILE_EMPTY);
        Require.notNull(partNumber, FileCode.FILE_UPLOAD_PART_INVALID);
        FileUploadSessionEntity session = selectUploadSession(sessionId);
        requireUploadSessionActive(session);
        Require.isTrue(FileUploadMode.SERVER_CHUNK.name().equals(session.getUploadMode()), FileCode.FILE_UPLOAD_SESSION_INVALID);
        Require.isTrue(partNumber >= 1 && partNumber <= session.getTotalParts(), FileCode.FILE_UPLOAD_PART_INVALID);
        Require.isFalse(file.isEmpty(), FileCode.FILE_EMPTY);
        Path target = serverChunkPath(session.getId(), partNumber);
        try (InputStream input = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return Require.fail(FileCode.FILE_STORE_FAILED);
        }
        upsertUploadPart(session, partNumber, file.getSize(), null, "server-" + partNumber);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> completeUploadPart(Long sessionId, CompleteFileUploadPartCommand command) {
        Require.notNull(command, FileCode.FILE_UPLOAD_PART_INVALID);
        FileUploadSessionEntity session = selectUploadSession(sessionId);
        requireUploadSessionActive(session);
        Require.isTrue(command.getPartNumber() <= session.getTotalParts(), FileCode.FILE_UPLOAD_PART_INVALID);
        upsertUploadPart(session, command.getPartNumber(), command.getPartSize(), command.getPartHash(), command.getEtag());
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<FileRecordVO> completeUploadSession(Long sessionId) {
        FileUploadSessionEntity session = selectUploadSession(sessionId);
        requireUploadSessionActive(session);
        List<FileUploadPartEntity> parts = fileUploadPartMapper.selectList(new LambdaQueryWrapper<FileUploadPartEntity>()
                .eq(FileUploadPartEntity::getSessionId, session.getId())
                .eq(FileUploadPartEntity::getStatus, PART_STATUS_COMPLETED)
                .orderByAsc(FileUploadPartEntity::getPartNumber));
        Require.isTrue(parts.size() == session.getTotalParts(), FileCode.FILE_UPLOAD_PART_INVALID);
        session.setStatus(FileUploadSessionStatus.COMPLETING.name());
        session.setUpdatedBy(MangoContextHolder.userId());
        session.setUpdatedTime(LocalDateTime.now());
        fileUploadSessionMapper.updateById(session);
        FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                session.getStorageConfigId(),
                session.getStorageType(),
                session.getBucketName());
        completePhysicalUpload(session, storageConfig, parts);
        FileObjectEntity fileObject = createFileObject(session.getTenantId(),
                storageConfig,
                session.getObjectName(),
                session.getFileHash(),
                session.getFileSize(),
                session.getContentType(),
                0L);
        createHashMapping(session.getTenantId(), storageConfig, session.getFileHash(), session.getFileSize(), fileObject, settingsService.current());
        FileRecord record = createFileRecord(session.getTenantId(),
                MangoContextHolder.userId(),
                fileObject,
                session.getFileName(),
                session.getFileExt(),
                session.getFileSize(),
                session.getContentType(),
                session.getFileHash(),
                session.getPurpose(),
                session.getAccessLevel(),
                session.getBizType(),
                session.getBizId(),
                session.getBizMeta(),
                session.getDirectoryId(),
                settingsService.current());
        fileRecordMapper.insert(record);
        incrementObjectRefCount(fileObject.getId());
        session.setObjectId(fileObject.getId());
        session.setFileRecordId(record.getId());
        session.setStatus(FileUploadSessionStatus.COMPLETED.name());
        session.setUpdatedTime(LocalDateTime.now());
        fileUploadSessionMapper.updateById(session);
        return R.ok(toVO(record));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> abortUploadSession(Long sessionId) {
        FileUploadSessionEntity session = selectUploadSession(sessionId);
        if (FileUploadMode.S3_MULTIPART.name().equals(session.getUploadMode())
                && StringUtils.hasText(session.getStorageUploadId())) {
            FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                    session.getStorageConfigId(),
                    session.getStorageType(),
                    session.getBucketName());
            fileStorageRouter.abortMultipartUpload(storageConfig, session.getObjectName(), session.getStorageUploadId());
        }
        if (FileUploadMode.SERVER_CHUNK.name().equals(session.getUploadMode())) {
            cleanupServerChunkSession(session.getId());
        }
        session.setStatus(FileUploadSessionStatus.ABORTED.name());
        session.setUpdatedBy(MangoContextHolder.userId());
        session.setUpdatedTime(LocalDateTime.now());
        return R.ok(fileUploadSessionMapper.updateById(session) > 0);
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
        wrapper.orderByDesc(FileRecord::getId);
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
                || FileRecordStatus.DELETED.value() == record.getStatus()
                || Integer.valueOf(1).equals(record.getArchived()), FileCode.FILE_NOT_FOUND);
        return record;
    }

    private List<FileRecord> selectVisible(List<Long> ids) {
        List<FileRecord> records = fileRecordMapper.selectList(tenantVisibleWrapper()
                .in(FileRecord::getId, ids)
                .eq(FileRecord::getArchived, 0)
                .eq(FileRecord::getStatus, FileRecordStatus.COMPLETED.value()));
        Require.notEmpty(records, FileCode.FILE_NOT_FOUND);
        return records;
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
        return record != null && record.getId() != null;
    }

    private void fillDirectAccess(FilePreviewVO vo, FileRecord record, FileSettingsVO settings) {
        vo.setDirectAccess(false);
        String fallbackUrl = fileAccessUrlAssembler.downloadUrl(record.getId());
        vo.setPreviewUrl(fallbackUrl);
        vo.setDownloadUrl(fallbackUrl);
        if (FileAccessMode.of(settings.getAccessMode()) != FileAccessMode.DIRECT) {
            fillConfiguredPreviewUrl(vo, record, settings);
            return;
        }
        StoredObject storedObject = resolveStoredObject(record);
        FileStorageConfig storageConfig = storedObject.storageConfig();
        String objectName = storedObject.objectName();
        if (!shouldUsePresignedUrl(record, settings)) {
            fileStorageRouter.publicGetUrl(storageConfig, objectName, record.getFileName())
                    .ifPresent(url -> {
                        String externalUrl = fileAccessUrlAssembler.externalize(url);
                        vo.setDirectAccess(true);
                        vo.setPreviewUrl(externalUrl);
                        vo.setDirectPreviewUrl(externalUrl);
                    });
            fileStorageRouter.publicDownloadUrl(storageConfig, objectName, record.getFileName())
                    .ifPresent(url -> {
                        String externalUrl = fileAccessUrlAssembler.externalize(url);
                        vo.setDirectAccess(true);
                        vo.setDownloadUrl(externalUrl);
                        vo.setDirectDownloadUrl(externalUrl);
                    });
            fillConfiguredPreviewUrl(vo, record, settings);
            return;
        }
        long previewExpireSeconds = positiveOrDefault(settings.getPreviewExpireSeconds(), 600L);
        long downloadExpireSeconds = positiveOrDefault(settings.getAccessTokenExpireSeconds(), 600L);
        fileStorageRouter.presignedGetUrl(storageConfig, objectName, record.getFileName(),
                        Duration.ofSeconds(previewExpireSeconds))
                .ifPresent(url -> {
                    String externalUrl = fileAccessUrlAssembler.externalize(url);
                    vo.setDirectAccess(true);
                    vo.setPreviewUrl(externalUrl);
                    vo.setDirectPreviewUrl(externalUrl);
                    vo.setDirectPreviewExpireSeconds(previewExpireSeconds);
                });
        fileStorageRouter.presignedDownloadUrl(storageConfig, objectName, record.getFileName(),
                        Duration.ofSeconds(downloadExpireSeconds))
                .ifPresent(url -> {
                    String externalUrl = fileAccessUrlAssembler.externalize(url);
                    vo.setDirectAccess(true);
                    vo.setDownloadUrl(externalUrl);
                    vo.setDirectDownloadUrl(externalUrl);
                    vo.setDirectDownloadExpireSeconds(downloadExpireSeconds);
                });
        fillConfiguredPreviewUrl(vo, record, settings);
    }

    private void fillConfiguredPreviewUrl(FilePreviewVO vo, FileRecord record, FileSettingsVO settings) {
        if (!requiresPreviewProvider(record, settings)) {
            return;
        }
        long expireSeconds = positiveOrDefault(settings.getPreviewExpireSeconds(), 600L);
        String sourceUrl = StringUtils.hasText(vo.getDirectDownloadUrl()) ? vo.getDirectDownloadUrl() : vo.getDownloadUrl();
        String previewUrl = FilePreviewUrlBuilder.build(settings.getPreviewProviderUrl(), record, sourceUrl, expireSeconds);
        vo.setPreviewUrl(fileAccessUrlAssembler.externalize(previewUrl));
    }

    private boolean requiresPreviewProvider(FileRecord record, FileSettingsVO settings) {
        return record != null && settings != null && StringUtils.hasText(settings.getPreviewProviderUrl());
    }

    private void fillDirectAccess(FileRecordVO vo, FileRecord record, FileSettingsVO settings) {
        String fallbackUrl = fileAccessUrlAssembler.downloadUrl(record.getId());
        vo.setUrl(fallbackUrl);
        vo.setPreviewUrl(fallbackUrl);
        vo.setDownloadUrl(fallbackUrl);
        vo.setDirectAccess(false);
        if (FileAccessMode.of(settings.getAccessMode()) != FileAccessMode.DIRECT) {
            return;
        }
        StoredObject storedObject = resolveStoredObject(record);
        FileStorageConfig storageConfig = storedObject.storageConfig();
        String objectName = storedObject.objectName();
        if (!shouldUsePresignedUrl(record, settings)) {
            fileStorageRouter.publicGetUrl(storageConfig, objectName, record.getFileName())
                    .ifPresent(url -> {
                        String externalUrl = fileAccessUrlAssembler.externalize(url);
                        vo.setDirectAccess(true);
                        vo.setUrl(externalUrl);
                        vo.setPreviewUrl(externalUrl);
                        vo.setDirectPreviewUrl(externalUrl);
                    });
            fileStorageRouter.publicDownloadUrl(storageConfig, objectName, record.getFileName())
                    .ifPresent(url -> {
                        String externalUrl = fileAccessUrlAssembler.externalize(url);
                        vo.setDirectAccess(true);
                        vo.setDownloadUrl(externalUrl);
                        vo.setDirectDownloadUrl(externalUrl);
                    });
            return;
        }
        long previewExpireSeconds = positiveOrDefault(settings.getPreviewExpireSeconds(), 600L);
        long downloadExpireSeconds = positiveOrDefault(settings.getAccessTokenExpireSeconds(), 600L);
        fileStorageRouter.presignedGetUrl(storageConfig, objectName, record.getFileName(),
                        Duration.ofSeconds(previewExpireSeconds))
                    .ifPresent(url -> {
                        String externalUrl = fileAccessUrlAssembler.externalize(url);
                        vo.setDirectAccess(true);
                        vo.setUrl(externalUrl);
                        vo.setPreviewUrl(externalUrl);
                        vo.setDirectPreviewUrl(externalUrl);
                        vo.setDirectPreviewExpireSeconds(previewExpireSeconds);
                    });
        fileStorageRouter.presignedDownloadUrl(storageConfig, objectName, record.getFileName(),
                        Duration.ofSeconds(downloadExpireSeconds))
                .ifPresent(url -> {
                    String externalUrl = fileAccessUrlAssembler.externalize(url);
                    vo.setDirectAccess(true);
                    vo.setDownloadUrl(externalUrl);
                    vo.setDirectDownloadUrl(externalUrl);
                    vo.setDirectDownloadExpireSeconds(downloadExpireSeconds);
                });
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

    private FileStorageConfig activeStorageConfig() {
        FileStorageConfig storageConfig = storageConfigService.activeConfig();
        Require.isTrue(Integer.valueOf(1).equals(storageConfig.getStatus()), FileCode.STORAGE_CONFIG_DISABLED);
        return storageConfig;
    }

    private long resolveChunkSize(Long requestedChunkSize) {
        long chunkSize = requestedChunkSize == null || requestedChunkSize <= 0 ? DEFAULT_CHUNK_SIZE : requestedChunkSize;
        return Math.max(chunkSize, MIN_CHUNK_SIZE);
    }

    private int resolveTotalParts(Integer requestedTotalParts, long fileSize, long chunkSize) {
        int calculated = (int) ((fileSize + chunkSize - 1) / chunkSize);
        if (requestedTotalParts != null) {
            Require.isTrue(requestedTotalParts == calculated, FileCode.FILE_UPLOAD_PART_INVALID);
        }
        Require.isTrue(calculated > 0 && calculated <= 10000, FileCode.FILE_UPLOAD_PART_INVALID);
        return calculated;
    }

    private FileUploadSessionEntity selectUploadSession(Long sessionId) {
        Require.notNull(sessionId, FileCode.FILE_UPLOAD_SESSION_NOT_FOUND);
        FileUploadSessionEntity session = fileUploadSessionMapper.selectOne(new LambdaQueryWrapper<FileUploadSessionEntity>()
                .eq(FileUploadSessionEntity::getTenantId, requireTenantId())
                .eq(FileUploadSessionEntity::getId, sessionId)
                .last("LIMIT 1"));
        Require.notNull(session, FileCode.FILE_UPLOAD_SESSION_NOT_FOUND);
        return session;
    }

    private void requireUploadSessionActive(FileUploadSessionEntity session) {
        Require.isFalse(FileUploadSessionStatus.COMPLETED.name().equals(session.getStatus())
                || FileUploadSessionStatus.ABORTED.name().equals(session.getStatus())
                || FileUploadSessionStatus.FAILED.name().equals(session.getStatus())
                || FileUploadSessionStatus.EXPIRED.name().equals(session.getStatus()), FileCode.FILE_UPLOAD_SESSION_INVALID);
        Require.isTrue(session.getExpiresAt() == null || session.getExpiresAt().isAfter(LocalDateTime.now()),
                FileCode.FILE_UPLOAD_SESSION_INVALID);
    }

    private void upsertUploadPart(FileUploadSessionEntity session,
                                  Integer partNumber,
                                  Long partSize,
                                  String partHash,
                                  String etag) {
        FileUploadPartEntity existing = fileUploadPartMapper.selectOne(new LambdaQueryWrapper<FileUploadPartEntity>()
                .eq(FileUploadPartEntity::getSessionId, session.getId())
                .eq(FileUploadPartEntity::getPartNumber, partNumber)
                .last("LIMIT 1"));
        boolean created = existing == null;
        FileUploadPartEntity part = created ? new FileUploadPartEntity() : existing;
        Long userId = MangoContextHolder.userId();
        part.setTenantId(session.getTenantId());
        part.setSessionId(session.getId());
        part.setPartNumber(partNumber);
        part.setPartSize(partSize);
        part.setPartHash(trimToNull(partHash));
        part.setEtag(trimToNull(etag));
        part.setStatus(PART_STATUS_COMPLETED);
        LocalDateTime now = LocalDateTime.now();
        part.setUpdatedBy(userId);
        part.setUpdatedTime(now);
        if (created) {
            part.setCreatedBy(userId);
            part.setCreatedTime(now);
            fileUploadPartMapper.insert(part);
            session.setUploadedParts(session.getUploadedParts() + 1);
        } else {
            fileUploadPartMapper.updateById(part);
        }
        session.setStatus(FileUploadSessionStatus.UPLOADING.name());
        session.setUpdatedBy(userId);
        session.setUpdatedTime(now);
        fileUploadSessionMapper.updateById(session);
    }

    private void completePhysicalUpload(FileUploadSessionEntity session,
                                        FileStorageConfig storageConfig,
                                        List<FileUploadPartEntity> parts) {
        if (FileUploadMode.S3_MULTIPART.name().equals(session.getUploadMode())) {
            List<CompletedUploadPart> completedParts = parts.stream()
                    .sorted(Comparator.comparing(FileUploadPartEntity::getPartNumber))
                    .map(item -> new CompletedUploadPart(item.getPartNumber(), item.getEtag()))
                    .collect(Collectors.toList());
            fileStorageRouter.completeMultipartUpload(storageConfig,
                    session.getObjectName(),
                    session.getStorageUploadId(),
                    completedParts);
            return;
        }
        Path merged = null;
        try {
            merged = Files.createTempFile("mango-file-merged-", ".upload");
            try (OutputStream output = Files.newOutputStream(merged)) {
                for (FileUploadPartEntity part : parts) {
                    Path partPath = serverChunkPath(session.getId(), part.getPartNumber());
                    Require.isTrue(Files.exists(partPath), FileCode.FILE_UPLOAD_PART_INVALID);
                    Files.copy(partPath, output);
                }
            }
            try (InputStream input = Files.newInputStream(merged)) {
                fileStorageRouter.putObject(storageConfig,
                        session.getObjectName(),
                        input,
                        Files.size(merged),
                        session.getContentType());
            }
            cleanupServerChunkSession(session.getId());
        } catch (IOException e) {
            Require.fail(FileCode.FILE_STORE_FAILED);
        } catch (Exception e) {
            Require.fail(FileCode.FILE_STORE_FAILED);
        } finally {
            if (merged != null) {
                try {
                    Files.deleteIfExists(merged);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响主流程结果。
                }
            }
        }
    }

    private Path serverChunkPath(Long sessionId, Integer partNumber) {
        return Path.of(System.getProperty("java.io.tmpdir"))
                .resolve("mango-file")
                .resolve("upload-session-" + sessionId)
                .resolve(partNumber + ".part")
                .toAbsolutePath()
                .normalize();
    }

    private void cleanupServerChunkSession(Long sessionId) {
        Path directory = Path.of(System.getProperty("java.io.tmpdir"))
                .resolve("mango-file")
                .resolve("upload-session-" + sessionId)
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(directory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // 临时分片清理失败不影响会话取消或完成结果。
        }
    }

    private Long normalizedStorageConfigId(FileStorageConfig storageConfig) {
        return storageConfig.getId() == null || storageConfig.getId() <= 0 ? 0L : storageConfig.getId();
    }

    private FileObjectEntity findInstantUploadObject(Long tenantId,
                                                     FileStorageConfig storageConfig,
                                                     String hash,
                                                     Long fileSize,
                                                     FileSettingsVO settings) {
        if (!Boolean.TRUE.equals(settings.getInstantUploadEnabled()) || !StringUtils.hasText(hash) || fileSize == null) {
            return null;
        }
        FileHashMappingEntity mapping = fileHashMappingMapper.selectOne(new LambdaQueryWrapper<FileHashMappingEntity>()
                .eq(FileHashMappingEntity::getScopeType, hashScope(settings).name())
                .eq(FileHashMappingEntity::getTenantId, hashTenantId(tenantId, settings))
                .eq(FileHashMappingEntity::getStorageConfigId, normalizedStorageConfigId(storageConfig))
                .eq(FileHashMappingEntity::getFileHash, hash)
                .eq(FileHashMappingEntity::getFileSize, fileSize)
                .eq(FileHashMappingEntity::getStatus, 1)
                .last("LIMIT 1"));
        if (mapping == null) {
            return null;
        }
        FileObjectEntity fileObject = fileObjectMapper.selectById(mapping.getObjectId());
        if (fileObject == null || !Integer.valueOf(FileObjectStatus.COMPLETED.value()).equals(fileObject.getStatus())) {
            return null;
        }
        return fileObject;
    }

    private FileObjectEntity findCompletedObject(FileStorageConfig storageConfig,
                                                 String hash,
                                                 Long fileSize) {
        if (!StringUtils.hasText(hash) || fileSize == null) {
            return null;
        }
        return fileObjectMapper.selectOne(new LambdaQueryWrapper<FileObjectEntity>()
                .eq(FileObjectEntity::getStorageConfigId, normalizedStorageConfigId(storageConfig))
                .eq(FileObjectEntity::getBucketName, storageConfig.getBucketName())
                .eq(FileObjectEntity::getFileHash, hash)
                .eq(FileObjectEntity::getFileSize, fileSize)
                .eq(FileObjectEntity::getStatus, FileObjectStatus.COMPLETED.value())
                .last("LIMIT 1"));
    }

    private FileObjectEntity createFileObject(Long tenantId,
                                              FileStorageConfig storageConfig,
                                              String objectName,
                                              String hash,
                                              Long fileSize,
                                              String contentType,
                                              Long refCount) {
        FileObjectEntity entity = new FileObjectEntity();
        entity.setTenantId(tenantId);
        entity.setStorageConfigId(normalizedStorageConfigId(storageConfig));
        entity.setStorageType(storageConfig.getStorageType());
        entity.setBucketName(storageConfig.getBucketName());
        entity.setObjectName(objectName);
        entity.setFileHash(hash);
        entity.setFileSize(fileSize);
        entity.setContentType(trimToNull(contentType));
        entity.setStatus(FileObjectStatus.COMPLETED.value());
        entity.setRefCount(refCount == null ? 0L : refCount);
        LocalDateTime now = LocalDateTime.now();
        Long userId = MangoContextHolder.userId();
        entity.setCreatedBy(userId);
        entity.setCreatedTime(now);
        entity.setUpdatedBy(userId);
        entity.setUpdatedTime(now);
        fileObjectMapper.insert(entity);
        return entity;
    }

    private void createHashMapping(Long tenantId,
                                   FileStorageConfig storageConfig,
                                   String hash,
                                   Long fileSize,
                                   FileObjectEntity fileObject,
                                   FileSettingsVO settings) {
        if (!Boolean.TRUE.equals(settings.getInstantUploadEnabled()) || !StringUtils.hasText(hash) || fileSize == null) {
            return;
        }
        FileHashMappingEntity existing = fileHashMappingMapper.selectOne(new LambdaQueryWrapper<FileHashMappingEntity>()
                .eq(FileHashMappingEntity::getScopeType, hashScope(settings).name())
                .eq(FileHashMappingEntity::getTenantId, hashTenantId(tenantId, settings))
                .eq(FileHashMappingEntity::getStorageConfigId, normalizedStorageConfigId(storageConfig))
                .eq(FileHashMappingEntity::getFileHash, hash)
                .eq(FileHashMappingEntity::getFileSize, fileSize)
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setObjectId(fileObject.getId());
            existing.setStatus(1);
            existing.setUpdatedBy(MangoContextHolder.userId());
            existing.setUpdatedTime(LocalDateTime.now());
            fileHashMappingMapper.updateById(existing);
            return;
        }
        FileHashMappingEntity mapping = new FileHashMappingEntity();
        mapping.setScopeType(hashScope(settings).name());
        mapping.setTenantId(hashTenantId(tenantId, settings));
        mapping.setStorageConfigId(normalizedStorageConfigId(storageConfig));
        mapping.setFileHash(hash);
        mapping.setFileSize(fileSize);
        mapping.setObjectId(fileObject.getId());
        mapping.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        Long userId = MangoContextHolder.userId();
        mapping.setCreatedBy(userId);
        mapping.setCreatedTime(now);
        mapping.setUpdatedBy(userId);
        mapping.setUpdatedTime(now);
        fileHashMappingMapper.insert(mapping);
    }

    private FileInstantUploadScope hashScope(FileSettingsVO settings) {
        return FileInstantUploadScope.of(settings.getInstantUploadScope()) == FileInstantUploadScope.GLOBAL
                ? FileInstantUploadScope.GLOBAL : FileInstantUploadScope.TENANT;
    }

    private Long hashTenantId(Long tenantId, FileSettingsVO settings) {
        return hashScope(settings) == FileInstantUploadScope.GLOBAL ? 0L : tenantId;
    }

    private FileRecord createFileRecord(Long tenantId,
                                        Long userId,
                                        FileObjectEntity fileObject,
                                        String fileName,
                                        String fileExt,
                                        Long fileSize,
                                        String contentType,
                                        String fileHash,
                                        String purpose,
                                        String accessLevel,
                                        String bizType,
                                        String bizId,
                                        String bizMeta,
                                        Long directoryId,
                                        FileSettingsVO settings) {
        FileRecord entity = new FileRecord();
        entity.setTenantId(tenantId);
        entity.setBizType(trimToNull(bizType));
        entity.setBizId(trimToNull(bizId));
        entity.setPurpose(trimToNull(purpose));
        entity.setBizMeta(bizMeta);
        entity.setDirectoryId(directoryId);
        entity.setAccessLevel(resolveAccessLevel(accessLevel, settings).name());
        entity.setObjectId(fileObject.getId());
        entity.setStorageType(fileObject.getStorageType());
        entity.setStorageConfigId(fileObject.getStorageConfigId());
        entity.setBucketName(fileObject.getBucketName());
        entity.setObjectName(fileObject.getObjectName());
        entity.setFileName(fileName);
        entity.setFileExt(fileExt);
        entity.setFileSize(fileSize);
        entity.setContentType(trimToNull(contentType));
        entity.setFileHash(fileHash);
        entity.setStatus(FileRecordStatus.COMPLETED.value());
        entity.setArchived(0);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        return entity;
    }

    private void incrementObjectRefCount(Long objectId) {
        Require.notNull(objectId, FileCode.FILE_NOT_FOUND);
        fileObjectMapper.update(null, new LambdaUpdateWrapper<FileObjectEntity>()
                .eq(FileObjectEntity::getId, objectId)
                .setSql("ref_count = ref_count + 1")
                .set(FileObjectEntity::getUpdatedTime, LocalDateTime.now()));
    }

    private void decrementObjectRefCount(Long objectId) {
        Require.notNull(objectId, FileCode.FILE_NOT_FOUND);
        fileObjectMapper.update(null, new LambdaUpdateWrapper<FileObjectEntity>()
                .eq(FileObjectEntity::getId, objectId)
                .gt(FileObjectEntity::getRefCount, 0)
                .setSql("ref_count = ref_count - 1")
                .set(FileObjectEntity::getUpdatedTime, LocalDateTime.now()));
    }

    private void markObjectUnreferenced(Long objectId) {
        fileObjectMapper.update(null, new LambdaUpdateWrapper<FileObjectEntity>()
                .eq(FileObjectEntity::getId, objectId)
                .set(FileObjectEntity::getStatus, FileObjectStatus.UNREFERENCED.value())
                .set(FileObjectEntity::getUpdatedTime, LocalDateTime.now()));
    }

    private void markObjectDeleted(Long objectId) {
        fileObjectMapper.update(null, new LambdaUpdateWrapper<FileObjectEntity>()
                .eq(FileObjectEntity::getId, objectId)
                .set(FileObjectEntity::getStatus, FileObjectStatus.DELETED.value())
                .set(FileObjectEntity::getUpdatedTime, LocalDateTime.now()));
    }

    private void disableHashMapping(Long objectId) {
        fileHashMappingMapper.update(null, new LambdaUpdateWrapper<FileHashMappingEntity>()
                .eq(FileHashMappingEntity::getObjectId, objectId)
                .set(FileHashMappingEntity::getStatus, 0)
                .set(FileHashMappingEntity::getUpdatedTime, LocalDateTime.now()));
    }

    private StoredObject resolveStoredObject(FileRecord record) {
        if (record.getObjectId() != null) {
            FileObjectEntity fileObject = fileObjectMapper.selectById(record.getObjectId());
            Require.notNull(fileObject, FileCode.FILE_NOT_FOUND);
            FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                    fileObject.getStorageConfigId(),
                    fileObject.getStorageType(),
                    fileObject.getBucketName());
            return new StoredObject(fileObject, storageConfig, fileObject.getObjectName());
        }
        FileStorageConfig storageConfig = storageConfigService.getEnabledConfig(
                record.getStorageConfigId(),
                record.getStorageType(),
                record.getBucketName());
        return new StoredObject(null, storageConfig, record.getObjectName());
    }

    private void removePhysicalObjectIfUnreferenced(FileRecord record) {
        StoredObject storedObject = resolveStoredObject(record);
        if (record.getObjectId() != null) {
            Long activeRefs = fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecord>()
                    .eq(FileRecord::getObjectId, record.getObjectId())
                    .eq(FileRecord::getArchived, 0)
                    .eq(FileRecord::getStatus, FileRecordStatus.COMPLETED.value()));
            if (activeRefs > 0) {
                return;
            }
            markObjectUnreferenced(record.getObjectId());
        } else {
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
        }
        try {
            fileStorageRouter.removeObject(storedObject.storageConfig(), storedObject.objectName());
            if (record.getObjectId() != null) {
                markObjectDeleted(record.getObjectId());
                disableHashMapping(record.getObjectId());
            }
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
        vo.setObjectId(entity.getObjectId());
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

    private record StoredObject(FileObjectEntity fileObject, FileStorageConfig storageConfig, String objectName) {
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
