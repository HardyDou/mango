package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.api.scope.DataScopeApplier;
import io.mango.infra.persistence.api.scope.DataScopeMapping;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.command.EnsureWorkflowDefinitionCommand;
import io.mango.workflow.api.command.SaveWorkflowDefinitionCommand;
import io.mango.workflow.api.command.UpdateWorkflowDefinitionStatusCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.query.WorkflowDefinitionPageQuery;
import io.mango.workflow.api.query.WorkflowDefinitionVersionQuery;
import io.mango.workflow.api.vo.WorkflowDefinitionVO;
import io.mango.workflow.api.vo.WorkflowDefinitionVersionVO;
import io.mango.workflow.api.vo.WorkflowDeployVO;
import io.mango.workflow.api.vo.WorkflowNodeCatalogVO;
import io.mango.workflow.core.engine.WorkflowDesignerBpmnConverter;
import io.mango.workflow.core.entity.WorkflowCategoryEntity;
import io.mango.workflow.core.entity.WorkflowDefinitionEntity;
import io.mango.workflow.core.entity.WorkflowDefinitionVersionEntity;
import io.mango.workflow.core.entity.WorkflowNodeDefinitionEntity;
import io.mango.workflow.core.mapper.WorkflowCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowDefinitionVersionMapper;
import io.mango.workflow.core.mapper.WorkflowNodeDefinitionMapper;
import io.mango.workflow.core.service.IWorkflowDefinitionService;
import io.mango.workflow.core.support.WorkflowDomainSupport;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 流程定义服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService implements IWorkflowDefinitionService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final String DEFINITION_LIST_RESOURCE_CODE = "workflow:definition:list";
    private static final DataScopeMapping DEFINITION_DATA_SCOPE_MAPPING = DataScopeMapping.builder()
            .tableName("workflow_definition")
            .selfField("created_by")
            .orgField("org_id")
            .tenantField("tenant_id")
            .build();

    private final WorkflowDefinitionMapper mapper;
    private final WorkflowCategoryMapper categoryMapper;
    private final WorkflowDefinitionVersionMapper versionMapper;
    private final WorkflowNodeDefinitionMapper nodeDefinitionMapper;
    private final DomainApi domainApi;
    private final RepositoryService repositoryService;
    private final WorkflowDesignerBpmnConverter bpmnConverter;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<DataScopeApplier> dataScopeApplierProvider;

    @Override
    public PageResult<WorkflowDefinitionVO> page(WorkflowDefinitionPageQuery query) {
        WorkflowDefinitionPageQuery resolved = query == null ? new WorkflowDefinitionPageQuery() : query;
        if (Boolean.TRUE.equals(resolved.getPublishedOnly())) {
            return publishedPage(resolved);
        }
        IPage<WorkflowDefinitionEntity> page = mapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<WorkflowDefinitionVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private PageResult<WorkflowDefinitionVO> publishedPage(WorkflowDefinitionPageQuery query) {
        IPage<WorkflowDefinitionEntity> page = mapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                publishedWrapper(query));
        List<WorkflowDefinitionVO> records = page.getRecords().stream()
                .map(this::publishedSnapshotVO)
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public WorkflowDefinitionVO get(Long id) {
        WorkflowDefinitionEntity entity = selectRequired(id);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SaveWorkflowDefinitionCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        validate(command, false);
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        copy(command, entity);
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(resolveTenantId());
        entity.setCreatedBy(MangoContextHolder.userId());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setCreatedTime(now);
        entity.setCreatedAt(now);
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        return String.valueOf(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(SaveWorkflowDefinitionCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        Require.notNull(command.getId(), WorkflowCode.DEFINITION_INVALID, "流程定义ID不能为空");
        validate(command, true);
        WorkflowDefinitionEntity entity = selectRequired(command.getId());
        copy(command, entity);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        WorkflowDefinitionEntity entity = selectRequired(id);
        Require.isTrue(!WorkflowDefinitionStatus.PUBLISHED.name().equals(entity.getStatus()),
                WorkflowCode.DEFINITION_STATUS_INVALID, "已发布流程定义请先停用再删除");
        return mapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(UpdateWorkflowDefinitionStatusCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        Require.notNull(command.getId(), WorkflowCode.DEFINITION_INVALID, "流程定义ID不能为空");
        WorkflowDefinitionStatus status = parseStatus(command.getStatus());
        WorkflowDefinitionEntity entity = selectRequired(command.getId());
        entity.setStatus(status.name());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean discardDraft(Long id) {
        WorkflowDefinitionEntity entity = selectRequired(id);
        WorkflowDefinitionVersionEntity latest = latestSuccessfulVersion(entity.getId());
        Require.notNull(latest, WorkflowCode.VERSION_NOT_FOUND, "暂无可回滚的已发布版本");
        entity.setCategoryId(latest.getCategoryId());
        entity.setDomainCode(latest.getDomainCode());
        entity.setOrgId(latest.getOrgId());
        entity.setAdminUsers(latest.getAdminUsers());
        entity.setStartEntryVisible(defaultStartEntryVisible(latest.getStartEntryVisible()));
        entity.setIcon(latest.getIcon());
        entity.setDefinitionName(latest.getDefinitionName());
        entity.setDefinitionKey(latest.getDefinitionKey());
        entity.setRemark(latest.getRemark());
        entity.setFormCode(latest.getFormCode());
        entity.setDesignerJson(latest.getDesignerJson());
        entity.setFormJson(latest.getFormJson());
        entity.setBpmnXml(latest.getBpmnXml());
        entity.setDeploymentId(latest.getDeploymentId());
        entity.setProcessDefinitionId(latest.getProcessDefinitionId());
        entity.setProcessDefinitionVersion(latest.getProcessDefinitionVersion());
        entity.setPublishedVersionNo(latest.getVersionNo());
        entity.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDeployVO deploy(Long id) {
        WorkflowDefinitionEntity entity = selectRequired(id);
        return deployDefinition(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDeployVO deployInternal(Long id) {
        WorkflowDefinitionEntity entity = selectInternalRequired(id);
        return deployDefinition(entity);
    }

    private WorkflowDeployVO deployDefinition(WorkflowDefinitionEntity entity) {
        Require.notBlank(entity.getDesignerJson(), WorkflowCode.DESIGNER_INVALID, "设计器JSON不能为空");
        WorkflowDefinitionVersionEntity version = null;
        try {
            BpmnModel bpmnModel = bpmnConverter.toModel(entity.getDesignerJson(), entity.getDefinitionKey(), entity.getDefinitionName());
            String bpmnXml = bpmnConverter.toXml(entity.getDesignerJson(), entity.getDefinitionKey(), entity.getDefinitionName());
            int nextVersionNo = nextVersionNo(entity.getId());
            LocalDateTime now = LocalDateTime.now();

            version = new WorkflowDefinitionVersionEntity();
            version.setTenantId(resolveTenantId());
            version.setDefinitionId(entity.getId());
            version.setVersionNo(nextVersionNo);
            version.setCategoryId(entity.getCategoryId());
            version.setDomainCode(entity.getDomainCode());
            version.setOrgId(entity.getOrgId());
            version.setAdminUsers(entity.getAdminUsers());
            version.setStartEntryVisible(defaultStartEntryVisible(entity.getStartEntryVisible()));
            version.setIcon(entity.getIcon());
            version.setDefinitionName(entity.getDefinitionName());
            version.setDefinitionKey(entity.getDefinitionKey());
            version.setRemark(entity.getRemark());
            version.setFormCode(entity.getFormCode());
            version.setDesignerJson(entity.getDesignerJson());
            version.setFormJson(entity.getFormJson());
            version.setBpmnXml(bpmnXml);
            version.setPublishStatus("PUBLISHING");
            version.setCreatedBy(MangoContextHolder.userId());
            version.setPublishTime(now);
            version.setCreatedTime(now);
            version.setCreatedAt(now);
            version.setUpdatedBy(MangoContextHolder.userId());
            version.setUpdatedTime(now);
            version.setUpdatedAt(now);
            versionMapper.insert(version);

            Deployment deployment = repositoryService.createDeployment()
                    .name(entity.getDefinitionName())
                    .key(entity.getDefinitionKey())
                    .tenantId(String.valueOf(resolveTenantId()))
                    .addBpmnModel(entity.getDefinitionKey() + ".bpmn20.xml", bpmnModel)
                    .deploy();
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .processDefinitionKey(entity.getDefinitionKey())
                    .latestVersion()
                    .singleResult();
            Require.notNull(processDefinition, WorkflowCode.DEPLOY_FAILED, "Flowable 未生成流程定义");
            entity.setDeploymentId(deployment.getId());
            entity.setProcessDefinitionId(processDefinition.getId());
            entity.setProcessDefinitionVersion(processDefinition.getVersion());
            entity.setPublishedVersionNo(nextVersionNo);
            entity.setBpmnXml(bpmnXml);
            entity.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
            entity.setLastDeployTime(now);
            entity.setUpdatedBy(MangoContextHolder.userId());
            entity.setUpdatedTime(now);
            entity.setUpdatedAt(now);
            mapper.updateById(entity);

            version.setDeploymentId(deployment.getId());
            version.setProcessDefinitionId(processDefinition.getId());
            version.setProcessDefinitionVersion(processDefinition.getVersion());
            version.setPublishStatus("SUCCESS");
            version.setPublishMessage("发布成功");
            version.setUpdatedTime(now);
            version.setUpdatedAt(now);
            versionMapper.updateById(version);

            WorkflowDeployVO vo = new WorkflowDeployVO();
            vo.setDeploymentId(deployment.getId());
            vo.setProcessDefinitionId(processDefinition.getId());
            vo.setProcessDefinitionVersion(processDefinition.getVersion());
            vo.setVersionNo(nextVersionNo);
            return vo;
        } catch (Exception e) {
            if (version != null && version.getId() != null) {
                version.setPublishStatus("FAILED");
                version.setPublishMessage(trimMessage(e.getMessage()));
                version.setUpdatedTime(LocalDateTime.now());
                version.setUpdatedAt(LocalDateTime.now());
                versionMapper.updateById(version);
            }
            return Require.fail(WorkflowCode.DEPLOY_FAILED, "流程发布失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDeployVO ensurePublished(EnsureWorkflowDefinitionCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        Long categoryId = ensureCategory(command);
        WorkflowDefinitionEntity definition = mapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                .eq(WorkflowDefinitionEntity::getTenantId, resolveTenantId())
                .eq(WorkflowDefinitionEntity::getDefinitionKey, command.getDefinitionKey().trim())
                .last("limit 1"));
        if (isPublishedAndDeployable(definition) && isEnsuredDefinitionCurrent(definition, command, categoryId)) {
            WorkflowDeployVO vo = new WorkflowDeployVO();
            vo.setDeploymentId(definition.getDeploymentId());
            vo.setProcessDefinitionId(definition.getProcessDefinitionId());
            vo.setProcessDefinitionVersion(definition.getProcessDefinitionVersion());
            vo.setVersionNo(definition.getPublishedVersionNo());
            return vo;
        }
        Long definitionId = definition == null
                ? createEnsuredDefinition(command, categoryId)
                : updateEnsuredDefinition(command, categoryId, definition);
        return deployInternal(definitionId);
    }

    @Override
    public List<WorkflowDefinitionVersionVO> versions(WorkflowDefinitionVersionQuery query) {
        Require.notNull(query, WorkflowCode.DEFINITION_INVALID);
        Require.notNull(query.getDefinitionId(), WorkflowCode.DEFINITION_INVALID, "流程定义ID不能为空");
        selectRequired(query.getDefinitionId());
        List<WorkflowDefinitionVersionVO> versions = versionMapper.selectList(
                        new LambdaQueryWrapper<WorkflowDefinitionVersionEntity>()
                                .eq(WorkflowDefinitionVersionEntity::getDefinitionId, query.getDefinitionId())
                                .orderByDesc(WorkflowDefinitionVersionEntity::getVersionNo))
                .stream()
                .map(this::toVersionVO)
                .toList();
        return versions;
    }

    @Override
    public WorkflowDefinitionVersionVO versionDetail(Long id) {
        Require.notNull(id, WorkflowCode.DEFINITION_INVALID, "发布版本ID不能为空");
        WorkflowDefinitionVersionEntity version = versionMapper.selectById(id);
        Require.notNull(version, WorkflowCode.VERSION_NOT_FOUND);
        selectRequired(version.getDefinitionId());
        return toVersionVO(version);
    }

    @Override
    public List<WorkflowNodeCatalogVO> nodeCatalog() {
        List<WorkflowNodeCatalogVO> nodes = nodeDefinitionMapper.selectList(new LambdaQueryWrapper<WorkflowNodeDefinitionEntity>()
                        .eq(WorkflowNodeDefinitionEntity::getStatus, 1)
                        .orderByAsc(WorkflowNodeDefinitionEntity::getCategoryCode)
                        .orderByAsc(WorkflowNodeDefinitionEntity::getSort)
                        .orderByAsc(WorkflowNodeDefinitionEntity::getId))
                .stream()
                .map(this::toNodeCatalogVO)
                .toList();
        return nodes;
    }

    private QueryWrapper<WorkflowDefinitionEntity> wrapper(WorkflowDefinitionPageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        QueryWrapper<WorkflowDefinitionEntity> wrapper = new QueryWrapper<WorkflowDefinitionEntity>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("definition_name", keyword)
                        .or()
                        .like("definition_key", keyword))
                .eq(query.getCategoryId() != null, "category_id", query.getCategoryId())
                .eq(StringUtils.hasText(query.getDomainCode()), "domain_code", trimToNull(query.getDomainCode()))
                .eq(query.getOrgId() != null, "org_id", query.getOrgId())
                .eq(query.getStartEntryVisible() != null, "start_entry_visible", query.getStartEntryVisible())
                .eq(StringUtils.hasText(query.getStatus()), "status", query.getStatus())
                .orderByDesc("updated_time");
        return applyDefinitionDataScope(wrapper);
    }

    private QueryWrapper<WorkflowDefinitionEntity> publishedWrapper(WorkflowDefinitionPageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        QueryWrapper<WorkflowDefinitionEntity> wrapper = new QueryWrapper<WorkflowDefinitionEntity>()
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like("definition_name", keyword)
                        .or()
                        .like("definition_key", keyword))
                .eq(query.getCategoryId() != null, "category_id", query.getCategoryId())
                .eq(StringUtils.hasText(query.getDomainCode()), "domain_code", trimToNull(query.getDomainCode()))
                .eq(query.getOrgId() != null, "org_id", query.getOrgId())
                .eq("status", WorkflowDefinitionStatus.PUBLISHED.name())
                .isNotNull("published_version_no")
                .isNotNull("process_definition_id")
                .exists(query.getStartEntryVisible() != null,
                        "SELECT 1 FROM workflow_definition_version wdv"
                                + " WHERE wdv.definition_id = workflow_definition.id"
                                + " AND wdv.version_no = workflow_definition.published_version_no"
                                + " AND wdv.publish_status = 'SUCCESS'"
                                + " AND wdv.start_entry_visible = {0}",
                        query.getStartEntryVisible())
                .orderByDesc("last_deploy_time");
        return applyDefinitionDataScope(wrapper);
    }

    private WorkflowDefinitionEntity selectRequired(Long id) {
        Require.notNull(id, WorkflowCode.DEFINITION_INVALID, "流程定义ID不能为空");
        QueryWrapper<WorkflowDefinitionEntity> wrapper = applyDefinitionDataScope(new QueryWrapper<WorkflowDefinitionEntity>()
                .eq("id", id)
                .last("limit 1"));
        WorkflowDefinitionEntity entity = mapper.selectOne(wrapper);
        Require.notNull(entity, WorkflowCode.DEFINITION_NOT_FOUND);
        return entity;
    }

    private WorkflowDefinitionEntity selectInternalRequired(Long id) {
        Require.notNull(id, WorkflowCode.DEFINITION_INVALID, "流程定义ID不能为空");
        WorkflowDefinitionEntity entity = mapper.selectOne(new QueryWrapper<WorkflowDefinitionEntity>()
                .eq("tenant_id", resolveTenantId())
                .eq("id", id)
                .last("limit 1"));
        Require.notNull(entity, WorkflowCode.DEFINITION_NOT_FOUND);
        return entity;
    }

    private QueryWrapper<WorkflowDefinitionEntity> applyDefinitionDataScope(QueryWrapper<WorkflowDefinitionEntity> wrapper) {
        DataScopeApplier dataScopeApplier = dataScopeApplierProvider.getIfAvailable();
        if (dataScopeApplier != null) {
            dataScopeApplier.apply(wrapper, DEFINITION_LIST_RESOURCE_CODE, DEFINITION_DATA_SCOPE_MAPPING);
        }
        return wrapper;
    }

    private void validate(SaveWorkflowDefinitionCommand command, boolean update) {
        validateDomain(command.getDomainCode());
        validateCategory(command.getCategoryId(), command.getDomainCode());
        Require.notBlank(command.getDefinitionName(), WorkflowCode.DEFINITION_INVALID, "流程名称不能为空");
        Require.notBlank(command.getDefinitionKey(), WorkflowCode.DEFINITION_INVALID, "流程编码不能为空");
        Require.notBlank(command.getDesignerJson(), WorkflowCode.DESIGNER_INVALID, "设计器JSON不能为空");
        Long count = mapper.selectCount(new LambdaQueryWrapper<WorkflowDefinitionEntity>()
                .eq(WorkflowDefinitionEntity::getDefinitionKey, command.getDefinitionKey().trim())
                .ne(update && command.getId() != null, WorkflowDefinitionEntity::getId, command.getId()));
        Require.isTrue(count == null || count == 0, WorkflowCode.DEFINITION_KEY_DUPLICATED);
        if (StringUtils.hasText(command.getStatus())) {
            parseStatus(command.getStatus());
        }
    }

    private Long ensureCategory(EnsureWorkflowDefinitionCommand command) {
        Require.notBlank(command.getDomainCode(), WorkflowCode.DEFINITION_INVALID, "业务域不能为空");
        Require.notBlank(command.getCategoryCode(), WorkflowCode.DEFINITION_INVALID, "流程分类编码不能为空");
        Require.notBlank(command.getCategoryName(), WorkflowCode.DEFINITION_INVALID, "流程分类名称不能为空");
        Require.notNull(command.getCategorySort(), WorkflowCode.DEFINITION_INVALID, "流程分类排序不能为空");
        validateDomain(command.getDomainCode());
        String domainCode = command.getDomainCode().trim();
        String categoryCode = command.getCategoryCode().trim();
        WorkflowCategoryEntity category = categoryMapper.selectOne(new LambdaQueryWrapper<WorkflowCategoryEntity>()
                .eq(WorkflowCategoryEntity::getTenantId, resolveTenantId())
                .eq(WorkflowCategoryEntity::getDomainCode, domainCode)
                .eq(WorkflowCategoryEntity::getCategoryCode, categoryCode)
                .last("limit 1"));
        if (category != null) {
            return category.getId();
        }
        LocalDateTime now = LocalDateTime.now();
        WorkflowCategoryEntity created = new WorkflowCategoryEntity();
        created.setTenantId(resolveTenantId());
        created.setCategoryName(command.getCategoryName().trim());
        created.setCategoryCode(categoryCode);
        created.setDomainCode(domainCode);
        created.setSort(command.getCategorySort());
        created.setStatus(1);
        created.setRemark(trimToNull(command.getCategoryRemark()));
        created.setCreatedBy(MangoContextHolder.userId());
        created.setUpdatedBy(MangoContextHolder.userId());
        created.setCreatedTime(now);
        created.setCreatedAt(now);
        created.setUpdatedTime(now);
        created.setUpdatedAt(now);
        categoryMapper.insert(created);
        return created.getId();
    }

    private Long createEnsuredDefinition(EnsureWorkflowDefinitionCommand command, Long categoryId) {
        SaveWorkflowDefinitionCommand definition = new SaveWorkflowDefinitionCommand();
        definition.setCategoryId(categoryId);
        definition.setDomainCode(command.getDomainCode());
        definition.setOrgId(command.getOrgId());
        definition.setAdminUsers(command.getAdminUsers());
        definition.setStartEntryVisible(command.getStartEntryVisible());
        definition.setIcon(command.getIcon());
        definition.setDefinitionName(command.getDefinitionName());
        definition.setDefinitionKey(command.getDefinitionKey());
        definition.setDesignerJson(command.getDesignerJson());
        definition.setFormCode(command.getFormCode());
        definition.setFormJson(command.getFormJson());
        definition.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        definition.setRemark(command.getRemark());
        String createResult = create(definition);
        Require.notBlank(createResult, WorkflowCode.DEFINITION_INVALID, "流程定义创建失败");
        return Long.valueOf(createResult);
    }

    private Long updateEnsuredDefinition(EnsureWorkflowDefinitionCommand command, Long categoryId,
                                         WorkflowDefinitionEntity definition) {
        SaveWorkflowDefinitionCommand updated = new SaveWorkflowDefinitionCommand();
        updated.setId(definition.getId());
        updated.setCategoryId(categoryId);
        updated.setDomainCode(command.getDomainCode());
        updated.setOrgId(command.getOrgId());
        updated.setAdminUsers(command.getAdminUsers());
        updated.setStartEntryVisible(command.getStartEntryVisible());
        updated.setIcon(command.getIcon());
        updated.setDefinitionName(command.getDefinitionName());
        updated.setDefinitionKey(command.getDefinitionKey());
        updated.setDesignerJson(command.getDesignerJson());
        updated.setFormCode(command.getFormCode());
        updated.setFormJson(command.getFormJson());
        updated.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        updated.setRemark(command.getRemark());
        validate(updated, true);
        copy(updated, definition);
        definition.setUpdatedBy(MangoContextHolder.userId());
        definition.setUpdatedTime(LocalDateTime.now());
        definition.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(definition);
        return definition.getId();
    }

    private boolean isEnsuredDefinitionCurrent(WorkflowDefinitionEntity definition, EnsureWorkflowDefinitionCommand command,
                                               Long categoryId) {
        if (definition == null) {
            return false;
        }
        return sameNumber(definition.getCategoryId(), categoryId)
                && sameText(definition.getDomainCode(), command.getDomainCode())
                && sameNumber(definition.getOrgId(), command.getOrgId())
                && sameText(definition.getAdminUsers(), toJsonList(command.getAdminUsers()))
                && defaultStartEntryVisible(definition.getStartEntryVisible())
                == defaultStartEntryVisible(command.getStartEntryVisible())
                && sameText(definition.getIcon(), command.getIcon())
                && sameText(definition.getDefinitionName(), command.getDefinitionName())
                && sameText(definition.getDefinitionKey(), command.getDefinitionKey())
                && sameText(definition.getDesignerJson(), command.getDesignerJson())
                && sameText(definition.getFormCode(), command.getFormCode())
                && sameText(definition.getFormJson(), command.getFormJson())
                && sameText(definition.getRemark(), command.getRemark());
    }

    private boolean isPublishedAndDeployable(WorkflowDefinitionEntity definition) {
        if (definition == null
                || !WorkflowDefinitionStatus.PUBLISHED.name().equals(definition.getStatus())
                || !StringUtils.hasText(definition.getProcessDefinitionId())) {
            return false;
        }
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definition.getProcessDefinitionId())
                .singleResult() != null;
    }

    private void copy(SaveWorkflowDefinitionCommand command, WorkflowDefinitionEntity entity) {
        entity.setCategoryId(command.getCategoryId() == null ? 0L : command.getCategoryId());
        entity.setDomainCode(command.getDomainCode().trim());
        entity.setOrgId(command.getOrgId());
        entity.setAdminUsers(toJsonList(command.getAdminUsers()));
        entity.setStartEntryVisible(defaultStartEntryVisible(command.getStartEntryVisible()));
        entity.setIcon(trimToNull(command.getIcon()));
        entity.setDefinitionName(command.getDefinitionName().trim());
        entity.setDefinitionKey(command.getDefinitionKey().trim());
        entity.setDesignerJson(command.getDesignerJson());
        entity.setBpmnXml(trimToNull(command.getBpmnXml()));
        entity.setFormCode(trimToNull(command.getFormCode()));
        entity.setFormJson(trimToNull(command.getFormJson()));
        entity.setStatus(StringUtils.hasText(command.getStatus())
                ? parseStatus(command.getStatus()).name()
                : WorkflowDefinitionStatus.DRAFT.name());
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private WorkflowDefinitionStatus parseStatus(String value) {
        try {
            return WorkflowDefinitionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Require.fail(WorkflowCode.DEFINITION_STATUS_INVALID);
        }
    }

    private WorkflowDefinitionVO toVO(WorkflowDefinitionEntity entity) {
        WorkflowDefinitionVO vo = new WorkflowDefinitionVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setCategoryName(resolveCategoryName(entity.getCategoryId()));
        vo.setDomainCode(entity.getDomainCode());
        vo.setOrgId(entity.getOrgId());
        vo.setAdminUsers(parseStringList(entity.getAdminUsers()));
        vo.setStartEntryVisible(defaultStartEntryVisible(entity.getStartEntryVisible()));
        vo.setIcon(entity.getIcon());
        vo.setDefinitionName(entity.getDefinitionName());
        vo.setDefinitionKey(entity.getDefinitionKey());
        vo.setDeploymentId(entity.getDeploymentId());
        vo.setProcessDefinitionId(entity.getProcessDefinitionId());
        vo.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        vo.setPublishedVersionNo(entity.getPublishedVersionNo());
        vo.setSourceTemplateId(entity.getSourceTemplateId());
        vo.setSourceTemplateCode(entity.getSourceTemplateCode());
        vo.setSourceTemplateVersion(entity.getSourceTemplateVersion());
        vo.setDesignerJson(entity.getDesignerJson());
        vo.setBpmnXml(entity.getBpmnXml());
        vo.setFormCode(entity.getFormCode());
        vo.setFormJson(entity.getFormJson());
        vo.setStatus(entity.getStatus());
        List<String> unpublishedChangeReasons = unpublishedChangeReasons(entity);
        vo.setHasUnpublishedChanges(!unpublishedChangeReasons.isEmpty());
        vo.setUnpublishedChangeReasons(unpublishedChangeReasons);
        vo.setLastDeployTime(entity.getLastDeployTime());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private WorkflowDefinitionVO publishedSnapshotVO(WorkflowDefinitionEntity entity) {
        WorkflowDefinitionVersionEntity version = latestSuccessfulVersion(entity.getId());
        Require.notNull(version, WorkflowCode.VERSION_NOT_FOUND, "已发布流程缺少发布版本快照");
        WorkflowDefinitionVO vo = new WorkflowDefinitionVO();
        vo.setId(entity.getId());
        vo.setCategoryId(version.getCategoryId());
        vo.setCategoryName(resolveCategoryName(version.getCategoryId()));
        vo.setDomainCode(version.getDomainCode());
        vo.setOrgId(version.getOrgId());
        vo.setAdminUsers(parseStringList(version.getAdminUsers()));
        vo.setStartEntryVisible(defaultStartEntryVisible(version.getStartEntryVisible()));
        vo.setIcon(version.getIcon());
        vo.setDefinitionName(version.getDefinitionName());
        vo.setDefinitionKey(version.getDefinitionKey());
        vo.setDeploymentId(version.getDeploymentId());
        vo.setProcessDefinitionId(version.getProcessDefinitionId());
        vo.setProcessDefinitionVersion(version.getProcessDefinitionVersion());
        vo.setPublishedVersionNo(version.getVersionNo());
        vo.setDesignerJson(version.getDesignerJson());
        vo.setBpmnXml(version.getBpmnXml());
        vo.setFormCode(version.getFormCode());
        vo.setFormJson(version.getFormJson());
        vo.setStatus(WorkflowDefinitionStatus.PUBLISHED.name());
        vo.setHasUnpublishedChanges(false);
        vo.setUnpublishedChangeReasons(List.of());
        vo.setLastDeployTime(version.getPublishTime());
        vo.setRemark(version.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(version.getPublishTime());
        return vo;
    }

    private Long resolveTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return 1L;
        }
        return Long.parseLong(tenantId);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateDomain(String domainCode) {
        WorkflowDomainSupport.requireEnabled(domainApi, domainCode);
    }

    private void validateCategory(Long categoryId, String domainCode) {
        Require.notNull(categoryId, WorkflowCode.DEFINITION_INVALID, "流程分类不能为空");
        WorkflowCategoryEntity category = categoryMapper.selectById(categoryId);
        Require.notNull(category, WorkflowCode.DEFINITION_INVALID, "流程分类不存在");
        Require.isTrue(Integer.valueOf(1).equals(category.getStatus()),
                WorkflowCode.DEFINITION_INVALID, "流程分类已停用");
        if (StringUtils.hasText(domainCode) && StringUtils.hasText(category.getDomainCode())) {
            Require.isTrue(domainCode.trim().equals(category.getDomainCode().trim()),
                    WorkflowCode.DEFINITION_INVALID, "流程分类不属于当前业务域");
        }
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null || categoryId == 0L) {
            return null;
        }
        WorkflowCategoryEntity category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getCategoryName();
    }

    private String toJsonList(Collection<String> values) {
        List<String> users = cleanList(values);
        if (users.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(users);
        } catch (JsonProcessingException e) {
            return String.join(",", users);
        }
    }

    private List<String> parseStringList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            List<String> users = objectMapper.readValue(value, STRING_LIST_TYPE);
            return cleanList(users);
        } catch (JsonProcessingException e) {
            return cleanList(List.of(value.split("\\s*,\\s*")));
        }
    }

    private List<String> cleanList(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                set.add(value.trim());
            }
        }
        return new ArrayList<>(set);
    }

    private int nextVersionNo(Long definitionId) {
        WorkflowDefinitionVersionEntity latest = versionMapper.selectOne(
                new LambdaQueryWrapper<WorkflowDefinitionVersionEntity>()
                        .eq(WorkflowDefinitionVersionEntity::getDefinitionId, definitionId)
                        .orderByDesc(WorkflowDefinitionVersionEntity::getVersionNo)
                        .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    private List<String> unpublishedChangeReasons(WorkflowDefinitionEntity entity) {
        if (entity == null || entity.getPublishedVersionNo() == null) {
            return List.of();
        }
        WorkflowDefinitionVersionEntity latest = latestSuccessfulVersion(entity.getId());
        if (latest == null) {
            return List.of();
        }
        List<String> reasons = new ArrayList<>();
        if (!sameText(entity.getDomainCode(), latest.getDomainCode())) {
            reasons.add("业务域");
        }
        if (!sameNumber(entity.getOrgId(), latest.getOrgId())) {
            reasons.add("所属组织");
        }
        if (!sameText(entity.getAdminUsers(), latest.getAdminUsers())) {
            reasons.add("流程管理员");
        }
        if (defaultStartEntryVisible(entity.getStartEntryVisible()) != defaultStartEntryVisible(latest.getStartEntryVisible())) {
            reasons.add("启动入口可见性");
        }
        if (!sameText(entity.getIcon(), latest.getIcon())) {
            reasons.add("流程图标");
        }
        if (!sameText(entity.getDefinitionName(), latest.getDefinitionName())) {
            reasons.add("流程名称");
        }
        if (!sameText(entity.getDefinitionKey(), latest.getDefinitionKey())) {
            reasons.add("流程编码");
        }
        if (!sameText(entity.getRemark(), latest.getRemark())) {
            reasons.add("流程备注");
        }
        if (!sameText(entity.getDesignerJson(), latest.getDesignerJson())) {
            reasons.add("流程设计");
        }
        if (!sameText(entity.getFormCode(), latest.getFormCode())) {
            reasons.add("表单编码");
        }
        if (!sameText(entity.getFormJson(), latest.getFormJson())) {
            reasons.add("表单配置");
        }
        return reasons;
    }

    private WorkflowDefinitionVersionEntity latestSuccessfulVersion(Long definitionId) {
        return versionMapper.selectOne(
                new LambdaQueryWrapper<WorkflowDefinitionVersionEntity>()
                        .eq(WorkflowDefinitionVersionEntity::getDefinitionId, definitionId)
                        .eq(WorkflowDefinitionVersionEntity::getPublishStatus, "SUCCESS")
                        .orderByDesc(WorkflowDefinitionVersionEntity::getVersionNo)
                        .last("LIMIT 1"));
    }

    private boolean sameText(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        return a.equals(b);
    }

    private boolean sameNumber(Long left, Long right) {
        long a = left == null ? 0L : left;
        long b = right == null ? 0L : right;
        return a == b;
    }

    private boolean defaultStartEntryVisible(Boolean value) {
        return value == null || value;
    }

    private WorkflowDefinitionVersionVO toVersionVO(WorkflowDefinitionVersionEntity entity) {
        WorkflowDefinitionVersionVO vo = new WorkflowDefinitionVersionVO();
        vo.setId(entity.getId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setCategoryId(entity.getCategoryId());
        vo.setDomainCode(entity.getDomainCode());
        vo.setOrgId(entity.getOrgId());
        vo.setAdminUsers(entity.getAdminUsers());
        vo.setStartEntryVisible(defaultStartEntryVisible(entity.getStartEntryVisible()));
        vo.setIcon(entity.getIcon());
        vo.setDefinitionName(entity.getDefinitionName());
        vo.setDefinitionKey(entity.getDefinitionKey());
        vo.setRemark(entity.getRemark());
        vo.setFormCode(entity.getFormCode());
        vo.setDesignerJson(entity.getDesignerJson());
        vo.setFormJson(entity.getFormJson());
        vo.setBpmnXml(entity.getBpmnXml());
        vo.setDeploymentId(entity.getDeploymentId());
        vo.setProcessDefinitionId(entity.getProcessDefinitionId());
        vo.setProcessDefinitionVersion(entity.getProcessDefinitionVersion());
        vo.setPublishStatus(entity.getPublishStatus());
        vo.setPublishMessage(entity.getPublishMessage());
        vo.setPublishTime(entity.getPublishTime());
        return vo;
    }

    private WorkflowNodeCatalogVO toNodeCatalogVO(WorkflowNodeDefinitionEntity entity) {
        return WorkflowNodeCatalogVO.of(
                entity.getNodeDefinitionCode(),
                entity.getNodeType(),
                entity.getNodeName(),
                entity.getCategoryCode(),
                entity.getCategoryName(),
                entity.getCategoryName(),
                entity.getDescription(),
                entity.getBpmnType(),
                entity.getExecutionType(),
                entity.getColor(),
                entity.getIcon(),
                entity.getPropertySchema(),
                entity.getDefaultProperties(),
                entity.getSort());
    }

    private String trimMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "发布失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
