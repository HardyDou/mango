package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileCode;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.enums.FileRecordStatus;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileRecord;
import io.mango.file.core.entity.FileStorageConfig;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.service.FileDownload;
import io.mango.file.core.service.IFileService;
import io.mango.file.core.service.IFileStorageConfigService;
import io.mango.file.core.storage.FileObject;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.infra.context.core.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件服务实现。
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileProperties properties;
    private final FileStorageRouter fileStorageRouter;
    private final IFileStorageConfigService storageConfigService;
    private final FileRecordMapper fileRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<FileRecordVO> upload(MultipartFile file,
                                  String purpose,
                                  String accessLevel,
                                  String bizType,
                                  String bizId) {
        validateUpload(file);
        Long tenantId = requireTenantId();
        Long userId = MangoContextHolder.userId();
        String originalFilename = normalizeFileName(file.getOriginalFilename());
        String fileExt = fileExt(originalFilename);
        validateExtension(fileExt);
        FileStorageConfig storageConfig = storageConfigService.activeConfig();
        Require.isTrue(Integer.valueOf(1).equals(storageConfig.getStatus()), FileCode.STORAGE_CONFIG_DISABLED);
        String objectName = generateObjectName(tenantId, fileExt);
        String hash;
        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                fileStorageRouter.putObject(storageConfig, objectName, digestInput, file.getSize(), file.getContentType());
            }
            hash = HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return Require.fail(FileCode.FILE_STORE_FAILED);
        }

        FileRecord entity = new FileRecord();
        entity.setTenantId(tenantId);
        entity.setBizType(trimToNull(bizType));
        entity.setBizId(trimToNull(bizId));
        entity.setPurpose(trimToNull(purpose));
        entity.setAccessLevel(FileAccessLevel.of(accessLevel).name());
        entity.setStorageType(storageConfig.getStorageType());
        entity.setStorageConfigId(storageConfig.getId() == null || storageConfig.getId() <= 0 ? null : storageConfig.getId());
        entity.setBucketName(storageConfig.getBucketName());
        entity.setObjectName(objectName);
        entity.setFileName(originalFilename);
        entity.setFileExt(fileExt);
        entity.setFileSize(file.getSize());
        entity.setContentType(trimToNull(file.getContentType()));
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
                                             String bizId) {
        Require.notEmpty(files == null ? List.of() : List.of(files), FileCode.FILE_EMPTY);
        List<FileRecordVO> result = List.of(files).stream()
                .filter(item -> item != null && !item.isEmpty())
                .map(item -> upload(item, purpose, accessLevel, bizType, bizId).getData())
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
        FilePreviewVO vo = new FilePreviewVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setFileExt(record.getFileExt());
        vo.setFileSize(record.getFileSize());
        vo.setContentType(record.getContentType());
        vo.setPreviewable(isPreviewable(record));
        vo.setPreviewUrl("/file/files/download?id=" + record.getId());
        vo.setDownloadUrl("/file/files/download?id=" + record.getId());
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
        Require.notNull(command, "归档命令不能为空");
        FileRecord record = selectVisible(command.getId());
        record.setStatus(FileRecordStatus.ARCHIVED.value());
        record.setArchived(1);
        record.setUpdatedBy(MangoContextHolder.userId());
        record.setUpdatedTime(LocalDateTime.now());
        return R.ok(fileRecordMapper.updateById(record) > 0);
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
        return wrapper;
    }

    private FileRecord selectVisible(Long id) {
        Require.notNull(id, "文件ID不能为空");
        FileRecord record = fileRecordMapper.selectOne(tenantVisibleWrapper().eq(FileRecord::getId, id).last("LIMIT 1"));
        Require.notNull(record, FileCode.FILE_NOT_FOUND);
        Require.isFalse(FileRecordStatus.ARCHIVED.value() == record.getStatus()
                || Integer.valueOf(1).equals(record.getArchived()), FileCode.FILE_NOT_FOUND);
        return record;
    }

    private void validateUpload(MultipartFile file) {
        Require.notNull(file, FileCode.FILE_EMPTY);
        Require.isFalse(file.isEmpty(), FileCode.FILE_EMPTY);
        Require.isTrue(file.getSize() <= properties.getUpload().getMaxSize(),
                FileCode.FILE_STORE_FAILED.getCode(), "文件大小超过限制");
    }

    private void validateExtension(String fileExt) {
        String ext = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
        List<String> blocked = properties.getUpload().getBlockedExtensions();
        Require.isFalse(blocked != null && blocked.stream().map(item -> item.toLowerCase(Locale.ROOT)).anyMatch(ext::equals),
                FileCode.FILE_STORE_FAILED.getCode(), "该文件类型禁止上传");
        List<String> allowed = properties.getUpload().getAllowedExtensions();
        Require.isTrue(allowed == null || allowed.isEmpty()
                        || allowed.stream().map(item -> item.toLowerCase(Locale.ROOT)).anyMatch(ext::equals),
                FileCode.FILE_STORE_FAILED.getCode(), "该文件类型不允许上传");
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
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.startsWith("image/")
                || contentType.startsWith("text/")
                || "application/pdf".equalsIgnoreCase(contentType);
    }

    private String generateObjectName(Long tenantId, String fileExt) {
        String datePath = LocalDateTime.now().format(DATE_PATH);
        String id = UUID.randomUUID().toString().replace("-", "");
        String suffix = StringUtils.hasText(fileExt) ? "." + fileExt : "";
        return "tenant-" + tenantId + "/" + datePath + "/" + id + suffix;
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

    private FileRecordVO toVO(FileRecord entity) {
        FileRecordVO vo = new FileRecordVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setPurpose(entity.getPurpose());
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
        return vo;
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
