package io.mango.resource.core.sync;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.core.entity.ResourceChangeLogEntity;
import io.mango.resource.core.entity.ResourceRegistryEntity;
import io.mango.resource.core.entity.ResourceSyncLogEntity;
import io.mango.resource.core.mapper.ResourceChangeLogMapper;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.mapper.ResourceSyncLogMapper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源注册中心仓储。
 */
@RequiredArgsConstructor
public class ResourceRegistryRepository {

    private final ResourceRegistryMapper registryMapper;
    private final ResourceSyncLogMapper syncLogMapper;
    private final ResourceChangeLogMapper changeLogMapper;

    public ResourceRegistryRow findByResourceId(String resourceId) {
        return toRow(registryMapper.selectByResourceId(resourceId));
    }

    public ResourceRegistryRow findByTypeAndBizKey(String resourceType, String bizKey) {
        return toRow(registryMapper.selectByTypeAndBizKey(resourceType, bizKey));
    }

    public List<ResourceRegistryRow> listByModule(String moduleCode) {
        return registryMapper.selectByModule(moduleCode)
                .stream()
                .map(this::toRow)
                .collect(Collectors.toList());
    }

    public Long insert(ResourceDeclaration declaration, String hash, Long targetId, String targetTable) {
        Long id = IdWorker.getId();
        LocalDateTime now = LocalDateTime.now();
        ResourceRegistryEntity entity = new ResourceRegistryEntity();
        entity.setId(id);
        entity.setResourceId(declaration.getId());
        entity.setResourceVersion(declaration.getVersion());
        entity.setResourceType(declaration.getResourceType());
        entity.setModuleCode(declaration.getModuleCode());
        entity.setBizKey(declaration.getBizKey());
        entity.setName(declaration.getName());
        entity.setTargetModule(declaration.getTargetModule());
        entity.setTargetTable(targetTable);
        entity.setTargetId(targetId);
        entity.setSourceHash(hash);
        entity.setSyncMode(declaration.getSyncMode().name());
        entity.setStatus(declaration.getStatus().name());
        entity.setLastSyncTime(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        registryMapper.insert(entity);
        return id;
    }

    public void update(ResourceRegistryRow row, ResourceDeclaration declaration, String hash, Long targetId, String targetTable) {
        ResourceRegistryEntity entity = new ResourceRegistryEntity();
        entity.setId(row.getId());
        entity.setResourceVersion(declaration.getVersion());
        entity.setModuleCode(declaration.getModuleCode());
        entity.setBizKey(declaration.getBizKey());
        entity.setName(declaration.getName());
        entity.setTargetModule(declaration.getTargetModule());
        entity.setTargetTable(targetTable);
        entity.setTargetId(targetId);
        entity.setSourceHash(hash);
        entity.setStatus(declaration.getStatus().name());
        entity.setLastSyncTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        registryMapper.updateById(entity);
    }

    public void updateStatus(ResourceRegistryRow row, String status, String hash) {
        ResourceRegistryEntity entity = new ResourceRegistryEntity();
        entity.setId(row.getId());
        entity.setStatus(status);
        entity.setSourceHash(hash);
        entity.setLastSyncTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        registryMapper.updateById(entity);
    }

    public void insertSyncLog(Long resourceRowId, String syncType, String result, String message) {
        ResourceSyncLogEntity entity = new ResourceSyncLogEntity();
        entity.setId(IdWorker.getId());
        entity.setResourceId(resourceRowId);
        entity.setSyncType(syncType);
        entity.setResult(result);
        entity.setMessage(message);
        entity.setCreatedAt(LocalDateTime.now());
        syncLogMapper.insert(entity);
    }

    public void insertChangeLog(Long resourceRowId, String changeType, String beforeContent, String afterContent) {
        ResourceChangeLogEntity entity = new ResourceChangeLogEntity();
        entity.setId(IdWorker.getId());
        entity.setResourceId(resourceRowId);
        entity.setChangeType(changeType);
        entity.setOperatorId(0L);
        entity.setBeforeContent(beforeContent);
        entity.setAfterContent(afterContent);
        entity.setCreatedAt(LocalDateTime.now());
        changeLogMapper.insert(entity);
    }

    private ResourceRegistryRow toRow(ResourceRegistryEntity entity) {
        if (entity == null) {
            return null;
        }
        ResourceRegistryRow row = new ResourceRegistryRow();
        row.setId(entity.getId());
        row.setResourceId(entity.getResourceId());
        row.setResourceVersion(entity.getResourceVersion());
        row.setResourceType(entity.getResourceType());
        row.setModuleCode(entity.getModuleCode());
        row.setBizKey(entity.getBizKey());
        row.setName(entity.getName());
        row.setTargetModule(entity.getTargetModule());
        row.setTargetTable(entity.getTargetTable());
        row.setTargetId(entity.getTargetId());
        row.setSourceHash(entity.getSourceHash());
        row.setSyncMode(ResourceSyncMode.valueOf(entity.getSyncMode()));
        row.setStatus(entity.getStatus());
        return row;
    }
}
