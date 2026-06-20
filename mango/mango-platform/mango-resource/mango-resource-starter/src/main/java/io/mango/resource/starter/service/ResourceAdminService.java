package io.mango.resource.starter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.resource.api.command.RegisterResourceDeclarationsCommand;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.starter.query.ResourceLogPageQuery;
import io.mango.resource.starter.query.ResourceRegistryPageQuery;
import io.mango.resource.starter.vo.ResourceChangeLogVO;
import io.mango.resource.starter.vo.ResourceRegistryVO;
import io.mango.resource.starter.vo.ResourceSyncLogVO;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.core.entity.ResourceChangeLogEntity;
import io.mango.resource.core.entity.ResourceRegistryEntity;
import io.mango.resource.core.entity.ResourceSyncLogEntity;
import io.mango.resource.core.mapper.ResourceChangeLogMapper;
import io.mango.resource.core.mapper.ResourceRegistryMapper;
import io.mango.resource.core.mapper.ResourceSyncLogMapper;
import io.mango.resource.core.sync.ResourceRegistrySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceAdminService {

    private final ResourceRegistryMapper registryMapper;
    private final ResourceSyncLogMapper syncLogMapper;
    private final ResourceChangeLogMapper changeLogMapper;
    private final ObjectProvider<ResourceHandler> handlers;
    private final ResourceRegistrySyncService syncService;

    public void registerDeclarations(RegisterResourceDeclarationsCommand command) {
        Require.notNull(command, "资源声明注册命令不能为空");
        Require.notBlank(command.getAppCode(), "来源应用不能为空");
        Require.notBlank(command.getServiceCode(), "来源服务不能为空");
        List<String> moduleCodes = command.getModuleCodes() == null ? new ArrayList<>() : command.getModuleCodes();
        List<ResourceDeclaration> declarations = command.getDeclarations() == null
                ? new ArrayList<>()
                : command.getDeclarations();
        boolean hasDeclarations = !declarations.isEmpty();
        boolean hasModuleCodes = !moduleCodes.isEmpty();
        Require.isTrue(hasDeclarations || hasModuleCodes, "资源声明和管理模块不能同时为空");
        syncService.syncRemote(command.getAppCode(), command.getServiceCode(), moduleCodes, declarations);
        log.info("Mango resource remote declarations registered: appCode={}, serviceCode={}, count={}",
                command.getAppCode(), command.getServiceCode(), declarations.size());
    }

    public void forceSync() {
        syncService.sync(true);
    }

    public void deleteResource(String resourceId, boolean physical) {
        Require.notBlank(resourceId, "资源ID不能为空");
        syncService.deleteResource(resourceId, physical);
    }

    public PageResult<ResourceRegistryVO> pageRegistries(ResourceRegistryPageQuery query) {
        Page<ResourceRegistryEntity> page = registryMapper.selectPage(
                Page.of(query.getPage(), query.getSize()),
                registryWrapper(query)
        );
        return PageResult.of(page.getRecords().stream().map(this::toRegistryVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

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

    public List<ResourceHandlerSpec> listHandlerSpecs() {
        return handlers.stream()
                .map(ResourceHandler::spec)
                .toList();
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
