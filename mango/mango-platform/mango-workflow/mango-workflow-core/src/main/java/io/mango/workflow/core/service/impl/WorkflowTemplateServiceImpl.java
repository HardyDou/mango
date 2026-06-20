package io.mango.workflow.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.vo.DomainVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.workflow.api.WorkflowCode;
import io.mango.workflow.api.command.CreateWorkflowDefinitionFromTemplateCommand;
import io.mango.workflow.api.command.CreateWorkflowTemplateFromDefinitionCommand;
import io.mango.workflow.api.command.ImportWorkflowTemplatesCommand;
import io.mango.workflow.api.command.PushWorkflowTemplatesCommand;
import io.mango.workflow.api.command.SaveWorkflowTemplateCommand;
import io.mango.workflow.api.enums.WorkflowDefinitionStatus;
import io.mango.workflow.api.enums.WorkflowTemplateStatus;
import io.mango.workflow.api.query.WorkflowTemplatePageQuery;
import io.mango.workflow.api.vo.WorkflowTemplateImportErrorVO;
import io.mango.workflow.api.vo.WorkflowTemplateImportVO;
import io.mango.workflow.api.vo.WorkflowTemplateVO;
import io.mango.workflow.core.entity.WorkflowDefinition;
import io.mango.workflow.core.entity.WorkflowTemplate;
import io.mango.workflow.core.entity.WorkflowTemplateCategory;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.core.mapper.WorkflowTemplateCategoryMapper;
import io.mango.workflow.core.mapper.WorkflowTemplateMapper;
import io.mango.workflow.core.service.IWorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 流程模板服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkflowTemplateServiceImpl implements IWorkflowTemplateService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final WorkflowTemplateMapper templateMapper;
    private final WorkflowDefinitionMapper definitionMapper;
    private final WorkflowTemplateCategoryMapper templateCategoryMapper;
    private final DomainApi domainApi;
    private final ObjectMapper objectMapper;

    @Override
    public R<PageResult<WorkflowTemplateVO>> page(WorkflowTemplatePageQuery query) {
        WorkflowTemplatePageQuery resolved = query == null ? new WorkflowTemplatePageQuery() : query;
        IPage<WorkflowTemplate> page = templateMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                wrapper(resolved));
        List<WorkflowTemplateVO> records = page.getRecords().stream()
                .map(item -> toVO(item, templateCategoryName(item.getTemplateCategoryId())))
                .toList();
        return R.ok(PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<WorkflowTemplateVO> get(Long id) {
        WorkflowTemplate template = selectRequired(id);
        return R.ok(toVO(template, templateCategoryName(template.getTemplateCategoryId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> create(SaveWorkflowTemplateCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        validate(command);
        WorkflowTemplate entity = new WorkflowTemplate();
        copy(command, entity);
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantId(resolveTenantId());
        entity.setVersionNo(resolveVersionNo(command.getVersionNo(), command.getTemplateCode()));
        markOldVersionsNotLatest(entity.getTemplateCode());
        entity.setLatestFlag(true);
        entity.setCreatedBy(MangoContextHolder.userId());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setCreatedTime(now);
        entity.setCreatedAt(now);
        entity.setUpdatedTime(now);
        entity.setUpdatedAt(now);
        templateMapper.insert(entity);
        return R.ok(String.valueOf(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        selectRequired(id);
        return R.ok(templateMapper.deleteById(id) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> createFromDefinition(CreateWorkflowTemplateFromDefinitionCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        Require.notNull(command.getDefinitionId(), WorkflowCode.DEFINITION_INVALID.getCode(), "流程定义ID不能为空");
        WorkflowDefinition definition = definitionMapper.selectById(command.getDefinitionId());
        Require.notNull(definition, WorkflowCode.DEFINITION_NOT_FOUND);
        String templateDomainCode = StringUtils.hasText(command.getCategoryCode())
                ? command.getCategoryCode().trim()
                : definition.getDomainCode();
        validateDomain(templateDomainCode);

        SaveWorkflowTemplateCommand template = new SaveWorkflowTemplateCommand();
        template.setTemplateName(command.getTemplateName());
        template.setTemplateCode(command.getTemplateCode());
        template.setTemplateCategoryId(command.getTemplateCategoryId());
        template.setCategoryCode(templateDomainCode);
        template.setCategoryName(StringUtils.hasText(command.getCategoryName())
                ? command.getCategoryName().trim()
                : templateDomainCode);
        template.setIcon(definition.getIcon());
        template.setAdminUsers(parseStringList(definition.getAdminUsers()));
        template.setDesignerJson(definition.getDesignerJson());
        template.setFormCode(definition.getFormCode());
        template.setFormJson(definition.getFormJson());
        template.setStatus(WorkflowTemplateStatus.ENABLED.name());
        template.setVersionNo(nextTemplateVersion(command.getTemplateCode()));
        template.setRemark(StringUtils.hasText(command.getRemark()) ? command.getRemark() : definition.getRemark());
        R<String> result = create(template);
        if (!result.isSuccess() || !StringUtils.hasText(result.getData())) {
            return result;
        }
        WorkflowTemplate entity = templateMapper.selectById(Long.valueOf(result.getData()));
        entity.setSourceDefinitionId(definition.getId());
        entity.setSourceDefinitionKey(definition.getDefinitionKey());
        entity.setSourceDefinitionName(definition.getDefinitionName());
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(entity);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> createDefinition(CreateWorkflowDefinitionFromTemplateCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        WorkflowTemplate template = selectRequired(command.getTemplateId());
        Require.isTrue(WorkflowTemplateStatus.ENABLED.name().equals(template.getStatus()),
                WorkflowCode.DEFINITION_STATUS_INVALID.getCode(), "流程模板不可导入");
        Long targetTenantId = resolveTargetTenantId(command.getTargetTenantId());
        validateDomain(command.getDomainCode());
        Require.notBlank(command.getDefinitionName(), WorkflowCode.DEFINITION_INVALID.getCode(), "流程名称不能为空");
        Require.notBlank(command.getDefinitionKey(), WorkflowCode.DEFINITION_INVALID.getCode(), "流程编码不能为空");
        Long count = definitionMapper.selectCount(new LambdaQueryWrapper<WorkflowDefinition>()
                .eq(WorkflowDefinition::getTenantId, targetTenantId)
                .eq(WorkflowDefinition::getDefinitionKey, command.getDefinitionKey().trim()));
        Require.isTrue(count == null || count == 0, WorkflowCode.DEFINITION_KEY_DUPLICATED);

        LocalDateTime now = LocalDateTime.now();
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setTenantId(targetTenantId);
        definition.setCategoryId(command.getCategoryId() == null ? 0L : command.getCategoryId());
        definition.setDomainCode(command.getDomainCode().trim());
        definition.setOrgId(command.getOrgId());
        definition.setAdminUsers(toJsonList(command.getAdminUsers() == null || command.getAdminUsers().isEmpty()
                ? parseStringList(template.getAdminUsers())
                : command.getAdminUsers()));
        definition.setIcon(template.getIcon());
        definition.setDefinitionName(command.getDefinitionName().trim());
        definition.setDefinitionKey(command.getDefinitionKey().trim());
        definition.setSourceTemplateId(template.getId());
        definition.setSourceTemplateCode(template.getTemplateCode());
        definition.setSourceTemplateVersion(template.getVersionNo());
        definition.setDesignerJson(template.getDesignerJson());
        definition.setFormCode(template.getFormCode());
        definition.setFormJson(template.getFormJson());
        definition.setStatus(WorkflowDefinitionStatus.DRAFT.name());
        definition.setRemark(StringUtils.hasText(command.getRemark()) ? command.getRemark().trim() : template.getRemark());
        definition.setCreatedBy(MangoContextHolder.userId());
        definition.setUpdatedBy(MangoContextHolder.userId());
        definition.setCreatedTime(now);
        definition.setCreatedAt(now);
        definition.setUpdatedTime(now);
        definition.setUpdatedAt(now);
        definitionMapper.insert(definition);
        return R.ok(String.valueOf(definition.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<WorkflowTemplateImportVO> importTemplates(ImportWorkflowTemplatesCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        validateDomain(command.getDomainCode());
        Long targetTenantId = resolveTargetTenantId(command.getTargetTenantId());

        List<WorkflowTemplate> templates = importTemplateList(command);
        Require.notEmpty(templates, WorkflowCode.TEMPLATE_IMPORT_FAILED.getCode(), "没有可导入的流程模板");
        WorkflowTemplateImportVO result = new WorkflowTemplateImportVO();
        List<WorkflowTemplateImportErrorVO> duplicated = duplicateDefinitionErrors(targetTenantId, templates);
        if (!duplicated.isEmpty()) {
            result.setErrors(duplicated);
            return R.fail(WorkflowCode.DEFINITION_KEY_DUPLICATED.getCode(), "目标租户下存在同编码流程，未执行导入", result);
        }

        LocalDateTime now = LocalDateTime.now();
        createDraftDefinitions(targetTenantId, command.getCategoryId(), command.getDomainCode(), command.getOrgId(), command.getAdminUsers(), templates, result, now);
        return R.ok(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<WorkflowTemplateImportVO> pushTemplates(PushWorkflowTemplatesCommand command) {
        Require.notNull(command, WorkflowCode.DEFINITION_INVALID);
        Require.notEmpty(command.getTargetTenantIds(), WorkflowCode.TEMPLATE_IMPORT_FAILED.getCode(), "目标租户不能为空");
        validateDomain(command.getDomainCode());

        List<Long> targetTenantIds = command.getTargetTenantIds().stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Require.notEmpty(targetTenantIds, WorkflowCode.TEMPLATE_IMPORT_FAILED.getCode(), "目标租户不能为空");

        ImportWorkflowTemplatesCommand importCommand = new ImportWorkflowTemplatesCommand();
        importCommand.setTemplateCategoryId(command.getTemplateCategoryId());
        importCommand.setTemplateIds(command.getTemplateIds());
        importCommand.setDomainCode(command.getDomainCode());
        List<WorkflowTemplate> templates = importTemplateList(importCommand);
        Require.notEmpty(templates, WorkflowCode.TEMPLATE_IMPORT_FAILED.getCode(), "没有可推送的流程模板");

        WorkflowTemplateImportVO result = new WorkflowTemplateImportVO();
        for (Long targetTenantId : targetTenantIds) {
            List<WorkflowTemplateImportErrorVO> duplicated = duplicateDefinitionErrors(targetTenantId, templates);
            if (!duplicated.isEmpty()) {
                duplicated.forEach(error -> error.setReason("目标租户 " + targetTenantId + " 下已存在同编码流程"));
                result.getErrors().addAll(duplicated);
            }
        }
        if (!result.getErrors().isEmpty()) {
            return R.fail(WorkflowCode.DEFINITION_KEY_DUPLICATED.getCode(), "目标租户下存在同编码流程，未执行推送", result);
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long targetTenantId : targetTenantIds) {
            createDraftDefinitions(targetTenantId, 0L, command.getDomainCode(), command.getOrgId(), command.getAdminUsers(), templates, result, now);
        }
        return R.ok(result);
    }

    private void createDraftDefinitions(
            Long targetTenantId,
            Long categoryId,
            String domainCode,
            Long orgId,
            List<String> adminUsers,
            List<WorkflowTemplate> templates,
            WorkflowTemplateImportVO result,
            LocalDateTime now) {
        for (WorkflowTemplate template : templates) {
            WorkflowDefinition definition = new WorkflowDefinition();
            definition.setTenantId(targetTenantId);
            definition.setCategoryId(categoryId == null ? 0L : categoryId);
            definition.setDomainCode(StringUtils.hasText(domainCode) ? domainCode.trim() : "WORKFLOW");
            definition.setOrgId(orgId);
            definition.setAdminUsers(toJsonList(adminUsers == null || adminUsers.isEmpty()
                    ? parseStringList(template.getAdminUsers())
                    : adminUsers));
            definition.setIcon(template.getIcon());
            definition.setDefinitionName(template.getTemplateName());
            definition.setDefinitionKey(template.getTemplateCode());
            definition.setSourceTemplateId(template.getId());
            definition.setSourceTemplateCode(template.getTemplateCode());
            definition.setSourceTemplateVersion(template.getVersionNo());
            definition.setDesignerJson(template.getDesignerJson());
            definition.setFormCode(template.getFormCode());
            definition.setFormJson(template.getFormJson());
            definition.setStatus(WorkflowDefinitionStatus.DRAFT.name());
            definition.setRemark(template.getRemark());
            definition.setCreatedBy(MangoContextHolder.userId());
            definition.setUpdatedBy(MangoContextHolder.userId());
            definition.setCreatedTime(now);
            definition.setCreatedAt(now);
            definition.setUpdatedTime(now);
            definition.setUpdatedAt(now);
            definitionMapper.insert(definition);
            result.getDefinitionIds().add(definition.getId());
        }
    }

    private LambdaQueryWrapper<WorkflowTemplate> wrapper(WorkflowTemplatePageQuery query) {
        String keyword = trimToNull(query.getKeyword());
        return new LambdaQueryWrapper<WorkflowTemplate>()
                .eq(WorkflowTemplate::getTenantId, resolveTenantId())
                .and(StringUtils.hasText(keyword), nested -> nested
                        .like(WorkflowTemplate::getTemplateName, keyword)
                        .or()
                        .like(WorkflowTemplate::getTemplateCode, keyword)
                        .or()
                        .like(WorkflowTemplate::getCategoryName, keyword))
                .eq(StringUtils.hasText(query.getStatus()), WorkflowTemplate::getStatus, query.getStatus())
                .eq(query.getTemplateCategoryId() != null, WorkflowTemplate::getTemplateCategoryId, query.getTemplateCategoryId())
                .eq(StringUtils.hasText(query.getCategoryCode()), WorkflowTemplate::getCategoryCode, query.getCategoryCode())
                .orderByDesc(WorkflowTemplate::getUpdatedTime);
    }

    private WorkflowTemplate selectRequired(Long id) {
        Require.notNull(id, WorkflowCode.DEFINITION_INVALID.getCode(), "流程模板ID不能为空");
        WorkflowTemplate entity = templateMapper.selectById(id);
        Require.notNull(entity, WorkflowCode.DEFINITION_NOT_FOUND.getCode(), "流程模板不存在");
        return entity;
    }

    private List<WorkflowTemplate> importTemplateList(ImportWorkflowTemplatesCommand command) {
        LambdaQueryWrapper<WorkflowTemplate> wrapper = new LambdaQueryWrapper<WorkflowTemplate>()
                .eq(WorkflowTemplate::getTenantId, resolveTenantId())
                .eq(WorkflowTemplate::getLatestFlag, true)
                .eq(WorkflowTemplate::getStatus, WorkflowTemplateStatus.ENABLED.name());
        boolean hasSelectedTemplates = command.getTemplateIds() != null && !command.getTemplateIds().isEmpty();
        if (command.getTemplateCategoryId() != null) {
            wrapper.eq(WorkflowTemplate::getTemplateCategoryId, command.getTemplateCategoryId());
        } else if (hasSelectedTemplates) {
            wrapper.in(WorkflowTemplate::getId, command.getTemplateIds());
        } else if (StringUtils.hasText(command.getDomainCode())) {
            wrapper.eq(WorkflowTemplate::getCategoryCode, command.getDomainCode().trim());
        } else {
            Require.notEmpty(command.getTemplateIds(), WorkflowCode.TEMPLATE_IMPORT_FAILED.getCode(), "请选择要导入的流程模板");
        }
        List<WorkflowTemplate> templates = templateMapper.selectList(wrapper).stream()
                .sorted(Comparator.comparing(WorkflowTemplate::getTemplateCode))
                .toList();
        if (command.getTemplateCategoryId() == null && hasSelectedTemplates) {
            List<Long> selectedIds = command.getTemplateIds().stream().distinct().toList();
            Require.isTrue(templates.size() == selectedIds.size(), WorkflowCode.TEMPLATE_IMPORT_FAILED.getCode(), "存在不可导入的流程模板，请确认模板已启用且为最新版本");
        }
        return templates;
    }

    private List<WorkflowTemplateImportErrorVO> duplicateDefinitionErrors(Long targetTenantId, List<WorkflowTemplate> templates) {
        List<String> templateCodes = templates.stream()
                .map(WorkflowTemplate::getTemplateCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (templateCodes.isEmpty()) {
            return List.of();
        }
        List<WorkflowDefinition> exists = definitionMapper.selectList(new LambdaQueryWrapper<WorkflowDefinition>()
                .eq(WorkflowDefinition::getTenantId, targetTenantId)
                .in(WorkflowDefinition::getDefinitionKey, templateCodes));
        if (exists.isEmpty()) {
            return List.of();
        }
        List<String> duplicatedKeys = exists.stream().map(WorkflowDefinition::getDefinitionKey).distinct().toList();
        return templates.stream()
                .filter(template -> duplicatedKeys.contains(template.getTemplateCode()))
                .map(template -> importError(template, "目标租户下已存在同编码流程"))
                .toList();
    }

    private WorkflowTemplateImportErrorVO importError(WorkflowTemplate template, String reason) {
        WorkflowTemplateImportErrorVO vo = new WorkflowTemplateImportErrorVO();
        vo.setTemplateId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setTemplateCode(template.getTemplateCode());
        vo.setReason(reason);
        return vo;
    }

    private void validate(SaveWorkflowTemplateCommand command) {
        Require.notBlank(command.getTemplateName(), WorkflowCode.DEFINITION_INVALID.getCode(), "模板名称不能为空");
        Require.notBlank(command.getTemplateCode(), WorkflowCode.DEFINITION_INVALID.getCode(), "模板编码不能为空");
        Require.notBlank(command.getDesignerJson(), WorkflowCode.DESIGNER_INVALID.getCode(), "设计器JSON不能为空");
        validateDomain(command.getCategoryCode());
        if (command.getTemplateCategoryId() != null) {
            Require.notNull(templateCategoryMapper.selectById(command.getTemplateCategoryId()), WorkflowCode.TEMPLATE_CATEGORY_NOT_FOUND);
        }
        Long count = templateMapper.selectCount(new LambdaQueryWrapper<WorkflowTemplate>()
                .eq(WorkflowTemplate::getTenantId, resolveTenantId())
                .eq(WorkflowTemplate::getTemplateCode, command.getTemplateCode().trim())
                .eq(command.getVersionNo() != null, WorkflowTemplate::getVersionNo, command.getVersionNo()));
        Require.isTrue(count == null || count == 0, WorkflowCode.DEFINITION_KEY_DUPLICATED.getCode(), "模板编码和版本已存在");
        if (StringUtils.hasText(command.getStatus())) {
            parseStatus(command.getStatus());
        }
    }

    private void copy(SaveWorkflowTemplateCommand command, WorkflowTemplate entity) {
        entity.setTemplateName(command.getTemplateName().trim());
        entity.setTemplateCode(command.getTemplateCode().trim());
        entity.setTemplateCategoryId(command.getTemplateCategoryId());
        entity.setCategoryCode(trimToNull(command.getCategoryCode()));
        entity.setCategoryName(trimToNull(command.getCategoryName()));
        entity.setIcon(trimToNull(command.getIcon()));
        entity.setAdminUsers(toJsonList(command.getAdminUsers()));
        entity.setDesignerJson(command.getDesignerJson());
        entity.setFormCode(trimToNull(command.getFormCode()));
        entity.setFormJson(trimToNull(command.getFormJson()));
        entity.setStatus(StringUtils.hasText(command.getStatus())
                ? parseStatus(command.getStatus()).name()
                : WorkflowTemplateStatus.ENABLED.name());
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private void validateDomain(String domainCode) {
        Require.notBlank(domainCode, WorkflowCode.DEFINITION_INVALID.getCode(), "业务域不能为空");
        R<DomainVO> response = domainApi.detailByCode(domainCode.trim());
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null,
                WorkflowCode.DEFINITION_INVALID.getCode(), "业务域不存在");
        Require.isTrue(Integer.valueOf(1).equals(response.getData().getStatus()),
                WorkflowCode.DEFINITION_INVALID.getCode(), "业务域已停用");
    }

    private WorkflowTemplateStatus parseStatus(String value) {
        try {
            return WorkflowTemplateStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Require.fail(WorkflowCode.DEFINITION_STATUS_INVALID);
        }
    }

    private int resolveVersionNo(Integer versionNo, String templateCode) {
        return versionNo == null || versionNo < 1 ? nextTemplateVersion(templateCode) : versionNo;
    }

    private int nextTemplateVersion(String templateCode) {
        if (!StringUtils.hasText(templateCode)) {
            return 1;
        }
        WorkflowTemplate latest = templateMapper.selectOne(new LambdaQueryWrapper<WorkflowTemplate>()
                .eq(WorkflowTemplate::getTenantId, resolveTenantId())
                .eq(WorkflowTemplate::getTemplateCode, templateCode.trim())
                .orderByDesc(WorkflowTemplate::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    private void markOldVersionsNotLatest(String templateCode) {
        if (!StringUtils.hasText(templateCode)) {
            return;
        }
        List<WorkflowTemplate> oldVersions = templateMapper.selectList(new LambdaQueryWrapper<WorkflowTemplate>()
                .eq(WorkflowTemplate::getTenantId, resolveTenantId())
                .eq(WorkflowTemplate::getTemplateCode, templateCode.trim())
                .eq(WorkflowTemplate::getLatestFlag, true));
        LocalDateTime now = LocalDateTime.now();
        for (WorkflowTemplate oldVersion : oldVersions) {
            oldVersion.setLatestFlag(false);
            oldVersion.setUpdatedBy(MangoContextHolder.userId());
            oldVersion.setUpdatedTime(now);
            oldVersion.setUpdatedAt(now);
            templateMapper.updateById(oldVersion);
        }
    }

    private WorkflowTemplateVO toVO(WorkflowTemplate entity, String templateCategoryName) {
        WorkflowTemplateVO vo = new WorkflowTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateCategoryId(entity.getTemplateCategoryId());
        vo.setTemplateCategoryName(templateCategoryName);
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setIcon(entity.getIcon());
        vo.setAdminUsers(parseStringList(entity.getAdminUsers()));
        vo.setDesignerJson(entity.getDesignerJson());
        vo.setFormCode(entity.getFormCode());
        vo.setFormJson(entity.getFormJson());
        vo.setVersionNo(entity.getVersionNo());
        vo.setLatestFlag(Boolean.TRUE.equals(entity.getLatestFlag()));
        vo.setStatus(entity.getStatus());
        vo.setStatusName(statusName(entity.getStatus()));
        vo.setSourceDefinitionId(entity.getSourceDefinitionId());
        vo.setSourceDefinitionKey(entity.getSourceDefinitionKey());
        vo.setSourceDefinitionName(entity.getSourceDefinitionName());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private String templateCategoryName(Long templateCategoryId) {
        if (templateCategoryId == null) {
            return null;
        }
        WorkflowTemplateCategory category = templateCategoryMapper.selectById(templateCategoryId);
        return category == null ? null : category.getCategoryName();
    }

    private String statusName(String status) {
        if (WorkflowTemplateStatus.DISABLED.name().equals(status)) {
            return "停用";
        }
        if (WorkflowTemplateStatus.ARCHIVED.name().equals(status)) {
            return "归档";
        }
        if (WorkflowTemplateStatus.DRAFT.name().equals(status)) {
            return "草稿";
        }
        if (WorkflowTemplateStatus.ENABLED.name().equals(status)) {
            return "启用";
        }
        return status;
    }

    private Long resolveTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        if (!StringUtils.hasText(tenantId)) {
            return 1L;
        }
        return Long.parseLong(tenantId);
    }

    private Long resolveTargetTenantId(Long targetTenantId) {
        return targetTenantId == null ? resolveTenantId() : targetTenantId;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
}
