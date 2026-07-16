package io.mango.resource.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.resource.api.query.ResourceLogPageQuery;
import io.mango.resource.api.enums.ResourceCode;
import io.mango.resource.api.query.ResourceRegistryPageQuery;
import io.mango.resource.api.vo.ResourceChangeLogVO;
import io.mango.resource.api.vo.ResourceHandlerFieldVO;
import io.mango.resource.api.vo.ResourceHandlerSpecVO;
import io.mango.resource.api.vo.ResourceRegistryVO;
import io.mango.resource.api.vo.ResourceSyncLogVO;
import io.mango.resource.core.entity.ResourceChangeLogEntity;
import io.mango.resource.core.entity.ResourceRegistryEntity;
import io.mango.resource.core.entity.ResourceSyncLogEntity;
import io.mango.resource.core.mapper.ResourceChangeLogMapper;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.mapper.ResourceSyncLogMapper;
import io.mango.resource.core.service.IResourceAdminService;
import io.mango.resource.core.service.IResourceRegistryService;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.model.ResourceHandlerSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceAdminService implements IResourceAdminService {

    private final ResourceRegistryMapper registryMapper;
    private final ResourceSyncLogMapper syncLogMapper;
    private final ResourceChangeLogMapper changeLogMapper;
    private final ObjectProvider<ResourceHandler> handlers;
    private final IResourceRegistryService registryService;

    @Override
    public Boolean forceSync() {
        registryService.sync(true);
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteResource(String resourceId, Boolean physical) {
        Require.notBlank(resourceId, ResourceCode.RESOURCE_INVALID, "资源ID不能为空");
        registryService.deleteResource(resourceId, Boolean.TRUE.equals(physical));
        return Boolean.TRUE;
    }

    @Override
    public PageResult<ResourceRegistryVO> pageRegistries(ResourceRegistryPageQuery query) {
        Page<ResourceRegistryEntity> page = registryMapper.selectPage(
                Page.of(query.getPage(), query.getSize()),
                registryWrapper(query)
        );
        return PageResult.of(page.getRecords().stream().map(this::toRegistryVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<ResourceSyncLogVO> pageSyncLogs(ResourceLogPageQuery query) {
        Page<ResourceSyncLogEntity> page = syncLogMapper.selectPage(
                Page.of(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<ResourceSyncLogEntity>()
                        .eq(query.getResourceId() != null, ResourceSyncLogEntity::getResourceId, query.getResourceId())
                        .orderByDesc(ResourceSyncLogEntity::getCreatedAt)
        );
        return PageResult.of(page.getRecords().stream().map(this::toSyncLogVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<ResourceChangeLogVO> pageChangeLogs(ResourceLogPageQuery query) {
        Page<ResourceChangeLogEntity> page = changeLogMapper.selectPage(
                Page.of(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<ResourceChangeLogEntity>()
                        .eq(query.getResourceId() != null, ResourceChangeLogEntity::getResourceId, query.getResourceId())
                        .orderByDesc(ResourceChangeLogEntity::getCreatedAt)
        );
        return PageResult.of(page.getRecords().stream().map(this::toChangeLogVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<ResourceHandlerSpecVO> listHandlerSpecs() {
        return handlers.stream()
                .map(ResourceHandler::spec)
                .map(this::toHandlerSpecVO)
                .toList();
    }

    private ResourceHandlerSpecVO toHandlerSpecVO(ResourceHandlerSpec spec) {
        ResourceHandlerSpecVO vo = new ResourceHandlerSpecVO();
        vo.setResourceType(spec.getResourceType());
        vo.setRequiredFields(spec.getRequiredFields().stream().sorted().toList());
        vo.setFields(spec.getFieldDescriptions().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> {
                    ResourceHandlerFieldVO field = new ResourceHandlerFieldVO();
                    field.setName(entry.getKey());
                    field.setDescription(entry.getValue());
                    return field;
                })
                .toList());
        return vo;
    }

    private LambdaQueryWrapper<ResourceRegistryEntity> registryWrapper(ResourceRegistryPageQuery query) {
        LambdaQueryWrapper<ResourceRegistryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getResourceType()), ResourceRegistryEntity::getResourceType, query.getResourceType());
        wrapper.eq(StringUtils.hasText(query.getModuleCode()), ResourceRegistryEntity::getModuleCode, query.getModuleCode());
        wrapper.eq(StringUtils.hasText(query.getTargetModule()), ResourceRegistryEntity::getTargetModule, query.getTargetModule());
        wrapper.eq(StringUtils.hasText(query.getSyncMode()), ResourceRegistryEntity::getSyncMode, query.getSyncMode());
        wrapper.eq(StringUtils.hasText(query.getStatus()), ResourceRegistryEntity::getStatus, query.getStatus());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(ResourceRegistryEntity::getResourceId, query.getKeyword())
                    .or().like(ResourceRegistryEntity::getBizKey, query.getKeyword())
                    .or().like(ResourceRegistryEntity::getName, query.getKeyword()));
        }
        wrapper.orderByDesc(ResourceRegistryEntity::getUpdatedAt);
        return wrapper;
    }

    private ResourceRegistryVO toRegistryVO(ResourceRegistryEntity entity) {
        ResourceRegistryVO vo = new ResourceRegistryVO();
        vo.setId(entity.getId());
        vo.setResourceId(entity.getResourceId());
        vo.setResourceVersion(entity.getResourceVersion());
        vo.setResourceType(entity.getResourceType());
        vo.setModuleCode(entity.getModuleCode());
        vo.setBizKey(entity.getBizKey());
        vo.setName(entity.getName());
        vo.setTargetModule(entity.getTargetModule());
        vo.setTargetTable(entity.getTargetTable());
        vo.setTargetId(entity.getTargetId());
        vo.setSourceHash(entity.getSourceHash());
        vo.setSyncMode(entity.getSyncMode());
        vo.setStatus(entity.getStatus());
        vo.setLastSyncTime(entity.getLastSyncTime());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private ResourceSyncLogVO toSyncLogVO(ResourceSyncLogEntity entity) {
        ResourceSyncLogVO vo = new ResourceSyncLogVO();
        vo.setId(entity.getId());
        vo.setResourceId(entity.getResourceId());
        vo.setSyncType(entity.getSyncType());
        vo.setResult(entity.getResult());
        vo.setMessage(entity.getMessage());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private ResourceChangeLogVO toChangeLogVO(ResourceChangeLogEntity entity) {
        ResourceChangeLogVO vo = new ResourceChangeLogVO();
        vo.setId(entity.getId());
        vo.setResourceId(entity.getResourceId());
        vo.setChangeType(entity.getChangeType());
        vo.setOperatorId(entity.getOperatorId());
        vo.setBeforeContent(entity.getBeforeContent());
        vo.setAfterContent(entity.getAfterContent());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
