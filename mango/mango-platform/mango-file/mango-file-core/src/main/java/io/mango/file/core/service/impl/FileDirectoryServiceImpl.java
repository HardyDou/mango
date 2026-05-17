package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.file.api.FileCode;
import io.mango.file.api.command.SaveFileDirectoryCommand;
import io.mango.file.api.vo.FileDirectoryVO;
import io.mango.file.core.entity.FileDirectory;
import io.mango.file.core.entity.FileRecord;
import io.mango.file.core.mapper.FileDirectoryMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.service.IFileDirectoryService;
import io.mango.infra.context.core.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件逻辑目录服务实现。
 */
@Service
@RequiredArgsConstructor
public class FileDirectoryServiceImpl implements IFileDirectoryService {

    private static final long ROOT_ID = 0L;

    private final FileDirectoryMapper directoryMapper;
    private final FileRecordMapper fileRecordMapper;

    @Override
    public R<List<FileDirectoryVO>> tree() {
        List<FileDirectory> directories = directoryMapper.selectList(new LambdaQueryWrapper<FileDirectory>()
                .eq(FileDirectory::getTenantId, requireTenantId())
                .orderByAsc(FileDirectory::getParentId)
                .orderByAsc(FileDirectory::getSort)
                .orderByDesc(FileDirectory::getCreatedTime));
        Map<Long, FileDirectoryVO> byId = new LinkedHashMap<>();
        for (FileDirectory item : directories) {
            byId.put(item.getId(), toVO(item));
        }
        List<FileDirectoryVO> roots = new ArrayList<>();
        for (FileDirectoryVO item : byId.values()) {
            FileDirectoryVO parent = byId.get(item.getParentId());
            if (parent == null || item.getParentId() == null || item.getParentId() == ROOT_ID) {
                roots.add(item);
            } else {
                parent.getChildren().add(item);
            }
        }
        sortTree(roots);
        return R.ok(roots);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(SaveFileDirectoryCommand command) {
        Require.notNull(command, FileCode.FILE_DIRECTORY_INVALID);
        Long tenantId = requireTenantId();
        Long parentId = normalizeParentId(command.getParentId());
        FileDirectory parent = parentId > ROOT_ID ? selectVisible(parentId) : null;
        validateName(command.getDirectoryName());
        requireUniqueName(tenantId, parentId, command.getDirectoryName(), null);

        LocalDateTime now = LocalDateTime.now();
        FileDirectory entity = new FileDirectory();
        entity.setTenantId(tenantId);
        entity.setParentId(parentId);
        entity.setDirectoryName(command.getDirectoryName().trim());
        entity.setSort(command.getSort() == null ? 0 : command.getSort());
        entity.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        entity.setCreatedBy(MangoContextHolder.userId());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        directoryMapper.insert(entity);
        entity.setDirectoryPath(buildPath(parent, entity.getId()));
        entity.setUpdatedTime(now);
        directoryMapper.updateById(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(SaveFileDirectoryCommand command) {
        Require.notNull(command, FileCode.FILE_DIRECTORY_INVALID);
        Require.notNull(command.getId(), FileCode.FILE_DIRECTORY_INVALID);
        FileDirectory entity = selectVisible(command.getId());
        validateName(command.getDirectoryName());
        requireUniqueName(entity.getTenantId(), entity.getParentId(), command.getDirectoryName(), entity.getId());
        entity.setDirectoryName(command.getDirectoryName().trim());
        entity.setSort(command.getSort() == null ? entity.getSort() : command.getSort());
        entity.setStatus(command.getStatus() == null ? entity.getStatus() : command.getStatus());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        return R.ok(directoryMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        Require.notNull(id, FileCode.FILE_DIRECTORY_INVALID);
        Require.isTrue(id > ROOT_ID, FileCode.FILE_ROOT_DIRECTORY_DELETE_FORBIDDEN);
        FileDirectory entity = selectVisible(id);
        Long childCount = directoryMapper.selectCount(new LambdaQueryWrapper<FileDirectory>()
                .eq(FileDirectory::getTenantId, entity.getTenantId())
                .eq(FileDirectory::getParentId, id));
        Long fileCount = fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getTenantId, entity.getTenantId())
                .eq(FileRecord::getDirectoryId, id)
                .eq(FileRecord::getArchived, 0));
        Require.isTrue(childCount == 0 && fileCount == 0, FileCode.FILE_DIRECTORY_NOT_EMPTY);
        return R.ok(directoryMapper.deleteById(id) > 0);
    }

    @Override
    public FileDirectory selectVisible(Long directoryId) {
        Long resolved = normalizeParentId(directoryId);
        if (resolved == ROOT_ID) {
            return null;
        }
        FileDirectory directory = directoryMapper.selectOne(new LambdaQueryWrapper<FileDirectory>()
                .eq(FileDirectory::getTenantId, requireTenantId())
                .eq(FileDirectory::getId, resolved)
                .eq(FileDirectory::getStatus, 1)
                .last("LIMIT 1"));
        Require.notNull(directory, FileCode.FILE_DIRECTORY_NOT_FOUND);
        return directory;
    }

    private void validateName(String value) {
        Require.notBlank(value, FileCode.FILE_DIRECTORY_INVALID);
        String trimmed = value.trim();
        Require.isFalse(trimmed.contains("/") || trimmed.contains("\\"), FileCode.FILE_DIRECTORY_INVALID);
        Require.isTrue(trimmed.length() <= 128, FileCode.FILE_DIRECTORY_INVALID);
    }

    private void requireUniqueName(Long tenantId, Long parentId, String directoryName, Long excludeId) {
        LambdaQueryWrapper<FileDirectory> wrapper = new LambdaQueryWrapper<FileDirectory>()
                .eq(FileDirectory::getTenantId, tenantId)
                .eq(FileDirectory::getParentId, parentId)
                .eq(FileDirectory::getDirectoryName, directoryName.trim());
        wrapper.ne(excludeId != null, FileDirectory::getId, excludeId);
        Require.isTrue(directoryMapper.selectCount(wrapper) == 0, FileCode.FILE_DIRECTORY_NAME_DUPLICATED);
    }

    private String buildPath(FileDirectory parent, Long id) {
        String parentPath = parent == null || !StringUtils.hasText(parent.getDirectoryPath()) ? "/" : parent.getDirectoryPath();
        return parentPath.endsWith("/") ? parentPath + id + "/" : parentPath + "/" + id + "/";
    }

    private void sortTree(List<FileDirectoryVO> nodes) {
        nodes.sort(Comparator.comparing((FileDirectoryVO item) -> item.getSort() == null ? 0 : item.getSort())
                .thenComparing(FileDirectoryVO::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder())));
        for (FileDirectoryVO node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private Long normalizeParentId(Long value) {
        return value == null || value < ROOT_ID ? ROOT_ID : value;
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

    private FileDirectoryVO toVO(FileDirectory entity) {
        FileDirectoryVO vo = new FileDirectoryVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setParentId(entity.getParentId());
        vo.setDirectoryName(entity.getDirectoryName());
        vo.setDirectoryPath(entity.getDirectoryPath());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }
}
