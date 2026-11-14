package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.file.api.command.CreateFileUploadSessionCommand;
import io.mango.file.api.command.ReplaceFileContentCommand;
import io.mango.file.api.command.UpdateFileRecordCommand;
import io.mango.file.api.enums.FileCode;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.api.vo.FileSettingsVO;
import io.mango.file.core.entity.FileObjectEntity;
import io.mango.file.core.entity.FileRecordEntity;
import io.mango.file.core.entity.FileUploadSessionEntity;
import io.mango.file.core.service.model.EnabledFileStorageKey;
import io.mango.infra.context.api.MangoContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * 文件信息编辑与内容替换操作。
 *
 * <p>从 {@link FileService} 拆出，避免单个实现类超过文件长度门禁；共享的物理对象、秒传映射和引用计数仍由
 * {@link FileService} 维护，本类只编排替换语义和文件信息更新。</p>
 */
final class FileEditOperations {

    private final FileService service;

    FileEditOperations(FileService service) {
        this.service = service;
    }

    FileRecordVO updateMetadata(UpdateFileRecordCommand command) {
        Require.notNull(command, FileCode.FILE_NOT_FOUND);
        Long tenantId = service.requireTenantId();
        FileSettingsVO settings = service.settingsService.current();
        FileRecordEntity record = selectVisibleForUpdate(command.getId());
        Long directoryId = command.getDirectoryId() == null
                ? record.getDirectoryId() : service.normalizeDirectoryId(command.getDirectoryId());
        if (!Objects.equals(directoryId, record.getDirectoryId())) {
            service.directoryService.selectVisible(directoryId);
        }
        String fileName = record.getFileName();
        if (command.getFileName() != null) {
            Require.isTrue(StringUtils.hasText(command.getFileName()), FileCode.FILE_EMPTY);
            String requested = FileService.normalizeFileName(command.getFileName());
            Require.isTrue(service.fileExt(requested).equals(service.fileExt(fileName)),
                    FileCode.FILE_REPLACE_EXTENSION_MISMATCH);
            if (!requested.equals(fileName)) {
                requireUniqueFileNameExcluding(tenantId,
                        service.duplicateDirectoryId(directoryId, settings), requested, record.getId());
                fileName = requested;
            }
        }
        record.setFileName(fileName);
        record.setDirectoryId(directoryId);
        record.setPurpose(metadataValue(command.getPurpose(), record.getPurpose()));
        record.setAccessLevel(command.getAccessLevel() == null
                ? record.getAccessLevel() : service.resolveAccessLevel(command.getAccessLevel(), settings).name());
        record.setBizType(metadataValue(command.getBizType(), record.getBizType()));
        record.setBizId(metadataValue(command.getBizId(), record.getBizId()));
        record.setBizMeta(command.getBizMeta() == null
                ? record.getBizMeta() : service.normalizeBizMeta(command.getBizMeta()));
        record.setUpdatedBy(MangoContextHolder.userId());
        record.setUpdatedTime(LocalDateTime.now());
        service.fileRecordMapper.updateById(record);
        return service.toVO(record);
    }

    FileRecordVO replaceContent(MultipartFile file, ReplaceFileContentCommand command) {
        Require.notNull(command, FileCode.FILE_NOT_FOUND);
        Require.notNull(file, FileCode.FILE_EMPTY);
        Require.isFalse(file.isEmpty(), FileCode.FILE_EMPTY);
        Long tenantId = service.requireTenantId();
        FileSettingsVO settings = service.settingsService.current();
        FileRecordEntity record = selectVisibleForUpdate(command.getId());
        Require.isTrue(Integer.valueOf(FileRecordStatus.COMPLETED.value()).equals(record.getStatus()),
                FileCode.FILE_STATUS_INVALID);
        String originalExt = record.getFileExt() == null ? "" : record.getFileExt().toLowerCase(Locale.ROOT);
        String uploadedName = FileService.normalizeFileName(file.getOriginalFilename());
        Require.isTrue(service.fileExt(uploadedName).equals(originalExt), FileCode.FILE_REPLACE_EXTENSION_MISMATCH);
        service.validateExtension(originalExt, settings);
        service.validateContentType(file.getContentType(), settings);
        long fileSize = file.getSize();
        Require.isTrue(fileSize <= settings.getMaxSize(), FileCode.FILE_SIZE_EXCEEDED);
        Long directoryId = command.getDirectoryId() == null
                ? record.getDirectoryId() : service.normalizeDirectoryId(command.getDirectoryId());
        if (!Objects.equals(directoryId, record.getDirectoryId())) {
            service.directoryService.selectVisible(directoryId);
        }
        String fileName = record.getFileName();
        if (command.getFileName() != null) {
            Require.isTrue(StringUtils.hasText(command.getFileName()), FileCode.FILE_EMPTY);
            String requested = FileService.normalizeFileName(command.getFileName());
            Require.isTrue(service.fileExt(requested).equals(originalExt), FileCode.FILE_REPLACE_EXTENSION_MISMATCH);
            if (!requested.equals(fileName)) {
                requireUniqueFileNameExcluding(tenantId,
                        service.duplicateDirectoryId(directoryId, settings), requested, record.getId());
                fileName = requested;
            }
        }
        record.setFileName(fileName);
        record.setDirectoryId(directoryId);
        record.setPurpose(metadataValue(command.getPurpose(), record.getPurpose()));
        record.setAccessLevel(command.getAccessLevel() == null
                ? record.getAccessLevel() : service.resolveAccessLevel(command.getAccessLevel(), settings).name());
        record.setBizType(metadataValue(command.getBizType(), record.getBizType()));
        record.setBizId(metadataValue(command.getBizId(), record.getBizId()));
        record.setBizMeta(command.getBizMeta() == null
                ? record.getBizMeta() : service.normalizeBizMeta(command.getBizMeta()));
        String hash = service.sha256(FileInput.fromMultipart(file));
        if (hash.equals(record.getFileHash()) && fileSize == record.getFileSize()) {
            record.setUpdatedBy(MangoContextHolder.userId());
            record.setUpdatedTime(LocalDateTime.now());
            service.fileRecordMapper.updateById(record);
            return service.toVO(record);
        }
        var storageConfig = service.storageConfigService.getEnabledConfig(new EnabledFileStorageKey(
                record.getStorageConfigId(), record.getStorageType(), record.getBucketName()));
        FileObjectEntity fileObject = service.findInstantUploadObject(tenantId, storageConfig, hash, fileSize, settings);
        if (fileObject == null) {
            String objectName = service.generateObjectName(
                    storageConfig, tenantId, directoryId, fileName, originalExt, hash, settings);
            try (InputStream uploadInput = file.getInputStream()) {
                service.fileStorageRouter.putObject(storageConfig, objectName, uploadInput, fileSize, file.getContentType());
            } catch (Exception e) {
                return Require.fail(FileCode.FILE_STORE_FAILED);
            }
            fileObject = service.createOrReuseFileObject(tenantId, storageConfig, objectName, hash,
                    fileSize, file.getContentType(), 0L);
            service.createHashMapping(tenantId, storageConfig, hash, fileSize, fileObject, settings);
        }
        return applyReplacement(record, fileObject, hash, fileSize, file.getContentType(), settings);
    }

    FileRecordEntity createSessionRecord(FileUploadSessionEntity session,
                                         FileObjectEntity fileObject,
                                         String resolvedFileHash) {
        FileRecordEntity record = service.createFileRecord(session.getTenantIdAsLong(),
                MangoContextHolder.userId(),
                fileObject,
                session.getFileName(),
                session.getFileExt(),
                session.getFileSize(),
                session.getContentType(),
                resolvedFileHash,
                session.getPurpose(),
                session.getAccessLevel(),
                session.getBizType(),
                session.getBizId(),
                session.getBizMeta(),
                session.getDirectoryId(),
                service.settingsService.current());
        service.fileRecordMapper.insert(record);
        service.incrementObjectRefCount(fileObject.getId());
        return record;
    }

    FileRecordEntity replaceSessionRecord(FileUploadSessionEntity session,
                                          FileObjectEntity fileObject,
                                          String resolvedFileHash) {
        FileSettingsVO settings = service.settingsService.current();
        FileRecordEntity record = selectVisibleForUpdate(session.getReplaceFileId());
        Require.isTrue(Integer.valueOf(FileRecordStatus.COMPLETED.value()).equals(record.getStatus()),
                FileCode.FILE_STATUS_INVALID);
        String targetExt = record.getFileExt() == null ? "" : record.getFileExt().toLowerCase(Locale.ROOT);
        Require.isTrue(service.fileExt(session.getFileName()).equals(targetExt),
                FileCode.FILE_REPLACE_EXTENSION_MISMATCH);
        Long directoryId = session.getDirectoryId() == null
                ? record.getDirectoryId() : service.normalizeDirectoryId(session.getDirectoryId());
        if (!Objects.equals(directoryId, record.getDirectoryId())) {
            service.directoryService.selectVisible(directoryId);
        }
        if (!session.getFileName().equals(record.getFileName())) {
            requireUniqueFileNameExcluding(record.getTenantIdAsLong(),
                    service.duplicateDirectoryId(directoryId, settings), session.getFileName(), record.getId());
        }
        record.setFileName(session.getFileName());
        record.setDirectoryId(directoryId);
        record.setPurpose(session.getPurpose());
        record.setAccessLevel(session.getAccessLevel());
        record.setBizType(session.getBizType());
        record.setBizId(session.getBizId());
        record.setBizMeta(session.getBizMeta());
        applyReplacement(record, fileObject, resolvedFileHash, session.getFileSize(), session.getContentType(), settings);
        return record;
    }

    void applySessionMetadata(FileRecordEntity record,
                              CreateFileUploadSessionCommand command,
                              String fileName,
                              Long directoryId,
                              String normalizedBizMeta,
                              FileSettingsVO settings) {
        record.setFileName(fileName);
        record.setDirectoryId(directoryId);
        record.setPurpose(metadataValue(command.getPurpose(), record.getPurpose()));
        record.setAccessLevel(command.getAccessLevel() == null
                ? record.getAccessLevel() : service.resolveAccessLevel(command.getAccessLevel(), settings).name());
        record.setBizType(metadataValue(command.getBizType(), record.getBizType()));
        record.setBizId(metadataValue(command.getBizId(), record.getBizId()));
        record.setBizMeta(normalizedBizMeta);
        record.setUpdatedBy(MangoContextHolder.userId());
        record.setUpdatedTime(LocalDateTime.now());
    }

    FileRecordVO applyReplacement(FileRecordEntity record,
                                          FileObjectEntity fileObject,
                                          String hash,
                                          Long fileSize,
                                          String contentType,
                                          FileSettingsVO settings) {
        FileRecordEntity previous = new FileRecordEntity();
        previous.setObjectId(record.getObjectId());
        previous.setStorageType(record.getStorageType());
        previous.setStorageConfigId(record.getStorageConfigId());
        previous.setBucketName(record.getBucketName());
        previous.setObjectName(record.getObjectName());
        boolean objectChanged = previous.getObjectId() == null
                || !previous.getObjectId().equals(fileObject.getId());
        record.setObjectId(fileObject.getId());
        record.setStorageType(fileObject.getStorageType());
        record.setStorageConfigId(fileObject.getStorageConfigId());
        record.setBucketName(fileObject.getBucketName());
        record.setObjectName(fileObject.getObjectName());
        record.setFileSize(fileSize);
        record.setContentType(service.trimToNull(contentType));
        record.setFileHash(hash);
        record.setUpdatedBy(MangoContextHolder.userId());
        record.setUpdatedTime(LocalDateTime.now());
        service.fileRecordMapper.updateById(record);
        if (!objectChanged) {
            return service.toVO(record);
        }
        service.incrementObjectRefCount(fileObject.getId());
        if (previous.getObjectId() != null) {
            service.decrementObjectRefCount(previous.getObjectId());
            if (Boolean.TRUE.equals(settings.getPhysicalDeleteEnabled())) {
                service.removePhysicalObjectIfUnreferenced(previous);
            }
        }
        return service.toVO(record);
    }

    FileRecordEntity selectVisibleForUpdate(Long id) {
        Require.notNull(id, FileCode.FILE_NOT_FOUND);
        FileRecordEntity record = service.fileRecordMapper.selectOne(service.tenantVisibleWrapper()
                .eq(FileRecordEntity::getId, id)
                .last("LIMIT 1 FOR UPDATE"));
        Require.notNull(record, FileCode.FILE_NOT_FOUND);
        Require.isFalse(FileRecordStatus.ARCHIVED.value() == record.getStatus()
                || FileRecordStatus.DELETED.value() == record.getStatus()
                || Integer.valueOf(1).equals(record.getArchived()), FileCode.FILE_NOT_FOUND);
        return record;
    }

    private void requireUniqueFileNameExcluding(Long tenantId,
                                                Long directoryId,
                                                String fileName,
                                                Long excludeId) {
        LambdaQueryWrapper<FileRecordEntity> wrapper = new LambdaQueryWrapper<FileRecordEntity>()
                .eq(FileRecordEntity::getTenantId, tenantId)
                .eq(FileRecordEntity::getFileName, fileName)
                .eq(FileRecordEntity::getArchived, 0)
                .ne(FileRecordEntity::getId, excludeId);
        wrapper.eq(directoryId != null, FileRecordEntity::getDirectoryId, directoryId);
        Require.isTrue(service.fileRecordMapper.selectCount(wrapper) == 0, FileCode.FILE_NAME_DUPLICATED);
    }

    String metadataValue(String incoming, String current) {
        return incoming == null ? current : service.trimToNull(incoming);
    }
}
