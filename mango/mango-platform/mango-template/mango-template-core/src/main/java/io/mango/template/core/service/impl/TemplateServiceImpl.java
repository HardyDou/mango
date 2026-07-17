package io.mango.template.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.template.api.enums.TemplateCode;
import io.mango.template.api.command.*;
import io.mango.template.api.enums.*;
import io.mango.template.api.query.TemplatePageQuery;
import io.mango.template.api.query.TemplateRenderRecordPageQuery;
import io.mango.template.api.vo.*;
import io.mango.template.core.entity.TemplateEntity;
import io.mango.template.core.entity.TemplateRenderRecordEntity;
import io.mango.template.core.entity.TemplateVersionEntity;
import io.mango.template.core.mapper.TemplateMapper;
import io.mango.template.core.mapper.TemplateRenderRecordMapper;
import io.mango.template.core.mapper.TemplateVersionMapper;
import io.mango.template.core.render.TemplateRenderManager;
import io.mango.template.core.render.TemplateRenderOutput;
import io.mango.template.core.render.TemplateRenderPayload;
import io.mango.template.core.service.ITemplateFileStore;
import io.mango.template.core.service.ITemplateDomainProvider;
import io.mango.template.core.service.ITemplateService;
import io.mango.template.core.service.TemplateDomainInfo;
import io.mango.template.core.service.TemplateJsonCodec;
import io.mango.template.core.service.TemplateStoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 模板服务实现。
 */
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl extends MangoCrudServiceImpl<TemplateMapper, TemplateEntity>
        implements ITemplateService {

    private static final TypeReference<List<TemplateVariableCommand>> VARIABLE_LIST_TYPE = new TypeReference<>() {
    };
    private static final String VARIABLE_PATH_SEPARATOR = ".";
    private static final String VARIABLE_PATH_SEPARATOR_REGEX = "\\.";

    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper versionMapper;
    private final TemplateRenderRecordMapper renderRecordMapper;
    private final TemplateRenderManager renderManager;
    private final ITemplateFileStore fileStore;
    private final Executor templateRenderExecutor;
    private final ITemplateDomainProvider domainProvider;

    @Override
    public PageResult<TemplateVO> pageResult(TemplatePageQuery query) {
        TemplatePageQuery resolved = query;
        if (resolved == null) {
            resolved = new TemplatePageQuery();
        }
        IPage<TemplateEntity> page = templateMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                templateWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PersistencePageResult<TemplateVO> page(TemplatePageQuery query) {
        PageResult<TemplateVO> result = pageResult(query);
        return PersistencePageResult.of(result.getList(), result.getTotal(), result.getPage(), result.getSize());
    }

    @Override
    public TemplateDetailVO detail(Long id) {
        TemplateEntity template = selectTemplate(id);
        TemplateDetailVO vo = toDetailVO(template);
        List<TemplateVersionEntity> versions = versionMapper.selectList(new LambdaQueryWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, template.getId())
                .orderByDesc(TemplateVersionEntity::getVersionNo));
        vo.setVersions(versions.stream().map(this::toVersionVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateTemplateCommand command) {
        validateSave(command);
        String tenantId = requireTenantId();
        Long userId = MangoContextHolder.userId();
        Require.isNull(templateMapper.selectOne(new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getTenantId, tenantId)
                .eq(TemplateEntity::getTemplateCode, command.getTemplateCode())
                .last("LIMIT 1")), TemplateCode.TEMPLATE_CODE_DUPLICATED);
        validateBusinessKeyUnique(tenantId, resolveBusinessKey(command), null);
        TemplateEntity entity = new TemplateEntity();
        entity.setTenantId(tenantId);
        entity.setStatus(TemplateStatus.ENABLED.value());
        entity.setCurrentVersionNo(0);
        entity.setHasUnpublishedChanges(0);
        applyTemplate(entity, command);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(UpdateTemplateCommand command) {
        validateSave(command);
        Require.notNull(command.getId(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板ID不能为空");
        TemplateEntity entity = selectTemplate(command.getId());
        validateBusinessKeyUnique(entity.getTenantId(), resolveBusinessKey(command), entity.getId());
        applyTemplate(entity, command);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        return templateMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        Require.notNull(id, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板ID不能为空");
        TemplateEntity entity = selectTemplate(id);
        renderRecordMapper.delete(new LambdaQueryWrapper<TemplateRenderRecordEntity>()
                .eq(TemplateRenderRecordEntity::getTemplateId, entity.getId()));
        versionMapper.delete(new LambdaQueryWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, entity.getId()));
        return templateMapper.deleteById(entity.getId()) > 0;
    }

    @Override
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板删除命令不能为空");
        Require.notNull(command.getId(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板ID不能为空");
        return delete(Long.valueOf(String.valueOf(command.getId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(UpdateTemplateStatusCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板状态命令不能为空");
        TemplateEntity entity = selectTemplate(command.getId());
        entity.setStatus(command.getStatus());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedAt(LocalDateTime.now());
        return templateMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishVersion(PublishTemplateVersionCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板版本命令不能为空");
        TemplateEntity template = selectTemplate(command.getTemplateId());
        validateVersionSource(command);
        Integer nextVersion = nextVersionNo(template.getId());
        versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, template.getId())
                .set(TemplateVersionEntity::getCurrentPublished, 0));
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setTenantId(template.getTenantId());
        version.setTemplateId(template.getId());
        version.setVersionNo(nextVersion);
        version.setSourceFormat(command.getSourceFormat().name());
        version.setContent(trimToNull(command.getContent()));
        version.setSourceFileId(command.getSourceFileId());
        version.setVariableSchema(toJson(command.getVariables()));
        version.setCurrentPublished(1);
        version.setVersionRemark(trimToNull(command.getVersionRemark()));
        version.setCreatedBy(MangoContextHolder.userId());
        version.setUpdatedBy(MangoContextHolder.userId());
        LocalDateTime now = LocalDateTime.now();
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        versionMapper.insert(version);
        template.setSourceFormat(command.getSourceFormat().name());
        template.setCurrentVersionNo(nextVersion);
        template.setDraftSourceFormat(command.getSourceFormat().name());
        template.setDraftContent(trimToNull(command.getContent()));
        template.setDraftSourceFileId(command.getSourceFileId());
        template.setDraftVariableSchema(toJson(command.getVariables()));
        template.setHasUnpublishedChanges(0);
        template.setUpdatedBy(MangoContextHolder.userId());
        template.setUpdatedAt(now);
        templateMapper.updateById(template);
        return version.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activateVersion(ActivateTemplateVersionCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "启用模板版本命令不能为空");
        TemplateEntity template = selectTemplate(command.getTemplateId());
        Require.notNull(command.getVersionNo(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板版本号不能为空");
        TemplateVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, template.getId())
                .eq(TemplateVersionEntity::getVersionNo, command.getVersionNo())
                .last("LIMIT 1"));
        Require.notNull(version, TemplateCode.TEMPLATE_VERSION_NOT_FOUND);
        versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, template.getId())
                .set(TemplateVersionEntity::getCurrentPublished, 0));
        version.setCurrentPublished(1);
        version.setUpdatedBy(MangoContextHolder.userId());
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        template.setSourceFormat(version.getSourceFormat());
        template.setCurrentVersionNo(version.getVersionNo());
        template.setDraftSourceFormat(version.getSourceFormat());
        template.setDraftContent(version.getContent());
        template.setDraftSourceFileId(version.getSourceFileId());
        template.setDraftVariableSchema(version.getVariableSchema());
        template.setHasUnpublishedChanges(0);
        template.setUpdatedBy(MangoContextHolder.userId());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
        return true;
    }

    @Override
    public List<String> extractVariables(ExtractTemplateVariablesCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "变量提取命令不能为空");
        Require.notNull(command.getSourceFormat(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板源格式不能为空");
        TemplateRenderPayload payload = payload(command.getSourceFormat(), TemplateOutputFormat.TEXT,
                command.getContent(), command.getSourceFileId(), Map.of());
        return renderManager.extractVariables(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateRenderResultVO render(TemplateRenderCommand command) {
        RenderContext context = prepareRender(command, TemplateRenderStatus.RUNNING);
        try {
            TemplateRenderResultVO result = doRender(context);
            markSuccess(context.record(), result);
            result.setRecordId(context.record().getId());
            result.setStatus(TemplateRenderStatus.SUCCESS.name());
            return result;
        } catch (Exception e) {
            markFailed(context.record(), e);
            Require.isTrue(false, TemplateCode.TEMPLATE_RENDER_FAILED, e.getMessage());
            return new TemplateRenderResultVO();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateRenderResultVO renderAsync(TemplateRenderCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板渲染命令不能为空");
        RenderContext context = prepareRender(command, TemplateRenderStatus.PENDING);
        MangoContextSnapshot snapshot = MangoContextHolder.get();
        templateRenderExecutor.execute(() -> {
            MangoContextHolder.set(snapshot);
            try {
                executeAsync(context.record().getId());
            } finally {
                MangoContextHolder.clear();
            }
        });
        TemplateRenderResultVO result = new TemplateRenderResultVO();
        result.setRecordId(context.record().getId());
        result.setStatus(TemplateRenderStatus.PENDING.name());
        return result;
    }

    public void executeAsync(Long recordId) {
        Require.notNull(recordId, TemplateCode.TEMPLATE_VALIDATION_ERROR, "渲染记录ID不能为空");
        TemplateRenderRecordEntity record = renderRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        try {
            record.setStatus(TemplateRenderStatus.RUNNING.name());
            record.setUpdatedAt(LocalDateTime.now());
            renderRecordMapper.updateById(record);
            TemplateEntity template = templateMapper.selectById(record.getTemplateId());
            TemplateVersionEntity version = versionMapper.selectById(record.getVersionId());
            Map<String, Object> variables = TemplateJsonCodec.read(record.getVariablePayload(), new TypeReference<>() {
            });
            TemplateRenderCommand command = new TemplateRenderCommand();
            command.setTemplateCode(record.getTemplateCode());
            command.setVersionNo(record.getVersionNo());
            command.setOutputFormat(TemplateOutputFormat.valueOf(record.getOutputFormat()));
            command.setVariables(TemplateJsonRequest.of(variables));
            command.setBizType(record.getBizType());
            command.setBizId(record.getBizId());
            TemplateRenderResultVO result = renderLoaded(template, version, command);
            markSuccess(record, result);
        } catch (Exception e) {
            markFailed(record, e);
        }
    }

    @Override
    public TemplateRenderRecordVO renderRecord(Long id) {
        Require.notNull(id, TemplateCode.TEMPLATE_VALIDATION_ERROR, "渲染记录ID不能为空");
        TemplateRenderRecordEntity record = renderRecordMapper.selectById(id);
        Require.notNull(record, TemplateCode.TEMPLATE_RENDER_RECORD_NOT_FOUND);
        return toRenderRecordVO(record);
    }

    @Override
    public PageResult<TemplateRenderRecordVO> renderRecordPage(TemplateRenderRecordPageQuery query) {
        TemplateRenderRecordPageQuery resolved = query;
        if (resolved == null) {
            resolved = new TemplateRenderRecordPageQuery();
        }
        IPage<TemplateRenderRecordEntity> page = renderRecordMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                recordWrapper(resolved));
        return PageResult.of(page.getRecords().stream().map(this::toRenderRecordVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    private RenderContext prepareRender(TemplateRenderCommand command, TemplateRenderStatus initialStatus) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板渲染命令不能为空");
        Require.notNull(command.getOutputFormat(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "输出格式不能为空");
        TemplateEntity template = selectTemplateForRender(command);
        Require.isTrue(TemplateStatus.ENABLED.value() == template.getStatus(), TemplateCode.TEMPLATE_DISABLED);
        TemplateVersionEntity version = selectVersion(template, command.getVersionNo());
        Map<String, Object> variables = renderVariables(command);
        validateRequiredVariables(version, variables);
        TemplateRenderRecordEntity record = new TemplateRenderRecordEntity();
        record.setTenantId(template.getTenantId());
        record.setTemplateId(template.getId());
        record.setTemplateCode(template.getTemplateCode());
        record.setVersionId(version.getId());
        record.setVersionNo(version.getVersionNo());
        record.setOutputFormat(command.getOutputFormat().name());
        record.setStatus(initialStatus.name());
        record.setVariablePayload(toVariableJson(variables));
        record.setBizType(trimToNull(command.getBizType()));
        record.setBizId(trimToNull(command.getBizId()));
        record.setCreatedBy(MangoContextHolder.userId());
        record.setUpdatedBy(MangoContextHolder.userId());
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        renderRecordMapper.insert(record);
        return new RenderContext(template, version, record, command);
    }

    private TemplateRenderResultVO doRender(RenderContext context) {
        return renderLoaded(context.template(), context.version(), context.command());
    }

    private TemplateRenderResultVO renderLoaded(TemplateEntity template, TemplateVersionEntity version, TemplateRenderCommand command) {
        TemplateRenderPayload payload = payload(TemplateSourceFormat.valueOf(version.getSourceFormat()),
                command.getOutputFormat(), version.getContent(), version.getSourceFileId(), renderVariables(command),
                parseVariables(version.getVariableSchema()));
        TemplateRenderOutput output = renderManager.render(payload);
        TemplateRenderResultVO result = new TemplateRenderResultVO();
        if (output.fileBytes() != null) {
            Long fileId = fileStore.save(output.fileBytes(), output.fileName(), output.contentType(),
                    "template-render", "template", template.getTemplateCode());
            result.setFileId(fileId);
            result.setFileName(output.fileName());
            result.setContentType(output.contentType());
        } else {
            result.setContent(output.content());
            result.setContentType(output.contentType());
        }
        return result;
    }

    private TemplateRenderPayload payload(TemplateSourceFormat sourceFormat,
                                          TemplateOutputFormat outputFormat,
                                          String content,
                                          Long sourceFileId,
                                          Map<String, Object> variables) {
        return payload(sourceFormat, outputFormat, content, sourceFileId, variables, List.of());
    }

    private TemplateRenderPayload payload(TemplateSourceFormat sourceFormat,
                                          TemplateOutputFormat outputFormat,
                                          String content,
                                          Long sourceFileId,
                                          Map<String, Object> variables,
                                          List<TemplateVariableCommand> variableDefinitions) {
        if (sourceFileId == null) {
            return new TemplateRenderPayload(sourceFormat, outputFormat, content, null, null, variables, variableDefinitions);
        }
        TemplateStoredFile file = fileStore.read(sourceFileId);
        return new TemplateRenderPayload(sourceFormat, outputFormat, content, toBytes(file), file.fileName(), variables,
                variableDefinitions);
    }

    private byte[] toBytes(TemplateStoredFile file) {
        try (InputStream input = file.inputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        } catch (IOException e) {
            Require.isTrue(false, TemplateCode.TEMPLATE_FILE_NOT_FOUND, "读取模板文件失败");
            return new byte[0];
        }
    }

    private void markSuccess(TemplateRenderRecordEntity record, TemplateRenderResultVO result) {
        record.setStatus(TemplateRenderStatus.SUCCESS.name());
        record.setOutputFileId(result.getFileId());
        record.setOutputContent(result.getContent());
        record.setErrorMessage(null);
        record.setUpdatedAt(LocalDateTime.now());
        renderRecordMapper.updateById(record);
    }

    private void markFailed(TemplateRenderRecordEntity record, Exception e) {
        record.setStatus(TemplateRenderStatus.FAILED.name());
        record.setErrorMessage(e.getMessage());
        record.setUpdatedAt(LocalDateTime.now());
        renderRecordMapper.updateById(record);
    }

    private void validateSave(SaveTemplateCommand command) {
        Require.notNull(command, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板保存命令不能为空");
        Require.notBlank(command.getTemplateCode(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板编码不能为空");
        Require.notBlank(command.getTemplateName(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板名称不能为空");
        validateDomain(command.getDomainCode());
    }

    private void validateVersionSource(PublishTemplateVersionCommand command) {
        Require.notNull(command.getSourceFormat(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "内容稿源格式不能为空");
        TemplateSourceFormat sourceFormat = command.getSourceFormat();
        if (sourceFormat == TemplateSourceFormat.TEXT || sourceFormat == TemplateSourceFormat.HTML) {
            Require.notBlank(command.getContent(), TemplateCode.TEMPLATE_VALIDATION_ERROR,
                    "文本/HTML 模板内容不能为空");
            return;
        }
        Require.notNull(command.getSourceFileId(), TemplateCode.TEMPLATE_VALIDATION_ERROR, "文档模板文件不能为空");
    }

    private void validateRequiredVariables(TemplateVersionEntity version, Map<String, Object> variables) {
        Map<String, Object> resolved = variables;
        if (resolved == null) {
            resolved = Map.of();
        }
        validateVariables(parseVariables(version.getVariableSchema()), resolved, "");
    }

    private void validateVariables(List<TemplateVariableCommand> definitions,
                                   Map<String, Object> variables,
                                   String parentPath) {
        validateVariables(definitions, variables, parentPath, parentPath);
    }

    private void validateVariables(List<TemplateVariableCommand> definitions,
                                   Map<String, Object> variables,
                                   String parentPath,
                                   String displayParentPath) {
        for (TemplateVariableCommand definition : definitions) {
            String resolvePath = variablePath(parentPath, definition.getName());
            String displayPath = variablePath(displayParentPath, definition.getName());
            if (!StringUtils.hasText(resolvePath)) {
                continue;
            }
            Object value = resolveVariable(resolvePath, variables);
            if (!Boolean.FALSE.equals(definition.getRequired()) && value == null) {
                Require.isTrue(false, TemplateCode.TEMPLATE_VARIABLE_MISSING, "缺少模板变量：" + displayPath);
            }
            validateVariableType(definition, displayPath, value);
            if (definition.getChildren() != null && !definition.getChildren().isEmpty()) {
                if (isArrayDefinition(definition) && value != null) {
                    validateArrayChildren(definition, displayPath, value);
                    continue;
                }
                validateVariables(definition.getChildren(), variables, resolvePath, displayPath);
            }
        }
    }

    private void validateArrayChildren(TemplateVariableCommand definition, String path, Object value) {
        Iterable<?> items = toIterable(value);
        int index = 0;
        for (Object item : items) {
            Require.isTrue(item instanceof Map<?, ?>, TemplateCode.TEMPLATE_VARIABLE_MISSING,
                    "模板变量类型不匹配：" + path + "[" + index + "]，期望 OBJECT");
            Map<?, ?> map = (Map<?, ?>) item;
            validateVariables(definition.getChildren(), castMap(map), "", path + "[" + index + "]");
            index++;
        }
    }

    private boolean isArrayDefinition(TemplateVariableCommand definition) {
        return "ARRAY".equalsIgnoreCase(Optional.ofNullable(definition.getType()).orElse(""));
    }

    private Iterable<?> toIterable(Object value) {
        if (value instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (value != null && value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> items = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                items.add(java.lang.reflect.Array.get(value, index));
            }
            return items;
        }
        return List.of();
    }

    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private void validateVariableType(TemplateVariableCommand definition, String path, Object value) {
        if (value == null) {
            return;
        }
        String type = Optional.ofNullable(definition.getType()).orElse("STRING").trim().toUpperCase(Locale.ROOT);
        boolean valid = isValidVariableType(type, value);
        if (!valid) {
            Require.isTrue(false, TemplateCode.TEMPLATE_VARIABLE_MISSING,
                    "模板变量类型不匹配：" + path + "，期望 " + type);
        }
    }

    private boolean isValidVariableType(String type, Object value) {
        return switch (type) {
            case "NUMBER" -> isValidNumber(value);
            case "BOOLEAN" -> isValidBoolean(value);
            case "OBJECT" -> value instanceof Map<?, ?>;
            case "ARRAY" -> value instanceof Collection<?> || value.getClass().isArray();
            case "DATE", "STRING" -> true;
            default -> true;
        };
    }

    private boolean isValidNumber(Object value) {
        return value instanceof Number || parseableNumber(value);
    }

    private boolean isValidBoolean(Object value) {
        return value instanceof Boolean || "true".equalsIgnoreCase(String.valueOf(value))
                || "false".equalsIgnoreCase(String.valueOf(value));
    }

    private boolean parseableNumber(Object value) {
        try {
            Double.parseDouble(String.valueOf(value));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String variablePath(String parentPath, String name) {
        String current = trimToNull(name);
        if (!StringUtils.hasText(parentPath)) {
            return current;
        }
        if (!StringUtils.hasText(current)) {
            return parentPath;
        }
        if (current.startsWith(parentPath + VARIABLE_PATH_SEPARATOR)) {
            return current;
        }
        return parentPath + VARIABLE_PATH_SEPARATOR + current;
    }

    private Object resolveVariable(String name, Map<String, Object> variables) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        Object current = variables;
        for (String part : name.split(VARIABLE_PATH_SEPARATOR_REGEX)) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Integer nextVersionNo(Long templateId) {
        TemplateVersionEntity latest = versionMapper.selectOne(new LambdaQueryWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, templateId)
                .orderByDesc(TemplateVersionEntity::getVersionNo)
                .last("LIMIT 1"));
        if (latest == null) {
            return 1;
        }
        return latest.getVersionNo() + 1;
    }

    private TemplateEntity selectTemplate(Long id) {
        Require.notNull(id, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板ID不能为空");
        TemplateEntity template = templateMapper.selectById(id);
        Require.notNull(template, TemplateCode.TEMPLATE_NOT_FOUND);
        Require.isTrue(Objects.equals(template.getTenantId(), requireTenantId()), TemplateCode.TEMPLATE_NOT_FOUND);
        return template;
    }

    private TemplateEntity selectTemplateByCode(String templateCode) {
        TemplateEntity template = templateMapper.selectOne(new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getTemplateCode, templateCode)
                .eq(TemplateEntity::getTenantId, requireTenantId())
                .last("LIMIT 1"));
        Require.notNull(template, TemplateCode.TEMPLATE_NOT_FOUND);
        return template;
    }

    private TemplateEntity selectTemplateForRender(TemplateRenderCommand command) {
        String templateCode = trimToNull(command.getTemplateCode());
        String businessKey = trimToNull(command.getBusinessKey());
        Require.isTrue(StringUtils.hasText(templateCode) || StringUtils.hasText(businessKey),
                TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板编码不能为空");
        Require.isTrue(!(StringUtils.hasText(templateCode) && StringUtils.hasText(businessKey)),
                TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板编码和兼容业务KEY只能传一个");
        if (StringUtils.hasText(templateCode)) {
            return selectTemplateByCode(templateCode);
        }
        TemplateEntity template = templateMapper.selectOne(new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getTenantId, requireTenantId())
                .eq(TemplateEntity::getBusinessKey, businessKey)
                .last("LIMIT 1"));
        Require.notNull(template, TemplateCode.TEMPLATE_NOT_FOUND);
        return template;
    }

    private TemplateVersionEntity selectVersion(TemplateEntity template, Integer versionNo) {
        LambdaQueryWrapper<TemplateVersionEntity> wrapper = new LambdaQueryWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, template.getId());
        if (versionNo == null) {
            wrapper.eq(TemplateVersionEntity::getCurrentPublished, 1);
        } else {
            wrapper.eq(TemplateVersionEntity::getVersionNo, versionNo);
        }
        TemplateVersionEntity version = versionMapper.selectOne(wrapper.last("LIMIT 1"));
        Require.notNull(version, TemplateCode.TEMPLATE_VERSION_NOT_FOUND);
        return version;
    }

    private LambdaQueryWrapper<TemplateEntity> templateWrapper(TemplatePageQuery query) {
        LambdaQueryWrapper<TemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateEntity::getTenantId, requireTenantId());
        String keyword = trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), item -> item
                .like(TemplateEntity::getTemplateCode, keyword)
                .or()
                .like(TemplateEntity::getTemplateName, keyword));
        wrapper.eq(StringUtils.hasText(query.getCategoryCode()), TemplateEntity::getCategoryCode, query.getCategoryCode());
        wrapper.eq(StringUtils.hasText(query.getDomainCode()), TemplateEntity::getDomainCode, trimToNull(query.getDomainCode()));
        wrapper.eq(StringUtils.hasText(query.getBusinessKey()), TemplateEntity::getBusinessKey, query.getBusinessKey());
        wrapper.eq(StringUtils.hasText(query.getSourceFormat()), TemplateEntity::getSourceFormat, query.getSourceFormat());
        wrapper.eq(query.getStatus() != null, TemplateEntity::getStatus, query.getStatus());
        wrapper.orderByDesc(TemplateEntity::getId);
        return wrapper;
    }

    private void validateBusinessKeyUnique(String tenantId, String businessKey, Long currentId) {
        if (!StringUtils.hasText(businessKey)) {
            return;
        }
        LambdaQueryWrapper<TemplateEntity> wrapper = new LambdaQueryWrapper<TemplateEntity>()
                .eq(TemplateEntity::getTenantId, tenantId)
                .eq(TemplateEntity::getBusinessKey, businessKey);
        if (currentId != null) {
            wrapper.ne(TemplateEntity::getId, currentId);
        }
        Require.isNull(templateMapper.selectOne(wrapper.last("LIMIT 1")), TemplateCode.TEMPLATE_BUSINESS_KEY_DUPLICATED);
    }

    private LambdaQueryWrapper<TemplateRenderRecordEntity> recordWrapper(TemplateRenderRecordPageQuery query) {
        LambdaQueryWrapper<TemplateRenderRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateRenderRecordEntity::getTenantId, requireTenantId());
        wrapper.eq(StringUtils.hasText(query.getTemplateCode()), TemplateRenderRecordEntity::getTemplateCode, query.getTemplateCode());
        wrapper.eq(StringUtils.hasText(query.getStatus()), TemplateRenderRecordEntity::getStatus, query.getStatus());
        wrapper.eq(StringUtils.hasText(query.getBizType()), TemplateRenderRecordEntity::getBizType, query.getBizType());
        wrapper.eq(StringUtils.hasText(query.getBizId()), TemplateRenderRecordEntity::getBizId, query.getBizId());
        wrapper.orderByDesc(TemplateRenderRecordEntity::getId);
        return wrapper;
    }

    private void applyTemplate(TemplateEntity entity, SaveTemplateCommand command) {
        entity.setTemplateCode(command.getTemplateCode().trim());
        entity.setTemplateName(command.getTemplateName().trim());
        TemplateDomainInfo domain = validateDomain(command.getDomainCode());
        entity.setDomainCode(domain.domainCode());
        entity.setCategoryCode(domain.domainCode());
        entity.setCategoryName(domain.domainName());
        entity.setBusinessGroup(trimToNull(command.getBusinessGroup()));
        entity.setBusinessType(trimToNull(command.getBusinessType()));
        entity.setBusinessKey(resolveBusinessKey(command));
        applyDraft(entity, command);
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private void applyDraft(TemplateEntity entity, SaveTemplateCommand command) {
        if (command.getSourceFormat() == null) {
            return;
        }
        entity.setDraftSourceFormat(parseSourceFormat(command.getSourceFormat()).name());
        entity.setDraftContent(trimToNull(command.getDraftContent()));
        entity.setDraftSourceFileId(command.getDraftSourceFileId());
        entity.setDraftVariableSchema(toJson(command.getDraftVariables()));
        if (hasDraftDifference(entity)) {
            entity.setHasUnpublishedChanges(1);
        } else {
            entity.setHasUnpublishedChanges(0);
        }
    }

    private boolean hasDraftDifference(TemplateEntity entity) {
        if (entity.getCurrentVersionNo() == null || entity.getCurrentVersionNo() <= 0) {
            return StringUtils.hasText(entity.getDraftSourceFormat())
                    || StringUtils.hasText(entity.getDraftContent())
                    || entity.getDraftSourceFileId() != null
                    || StringUtils.hasText(entity.getDraftVariableSchema());
        }
        TemplateVersionEntity current = versionMapper.selectOne(new LambdaQueryWrapper<TemplateVersionEntity>()
                .eq(TemplateVersionEntity::getTemplateId, entity.getId())
                .eq(TemplateVersionEntity::getVersionNo, entity.getCurrentVersionNo())
                .last("LIMIT 1"));
        if (current == null) {
            return true;
        }
        return !Objects.equals(trimToNull(entity.getDraftSourceFormat()), trimToNull(current.getSourceFormat()))
                || !Objects.equals(trimToNull(entity.getDraftContent()), trimToNull(current.getContent()))
                || !Objects.equals(entity.getDraftSourceFileId(), current.getSourceFileId())
                || !Objects.equals(normalizeJson(entity.getDraftVariableSchema()), normalizeJson(current.getVariableSchema()));
    }

    private String normalizeJson(String json) {
        if (!StringUtils.hasText(json)) {
            return "[]";
        }
        try {
            return TemplateJsonCodec.write(TemplateJsonCodec.readTree(json));
        } catch (Exception e) {
            return json;
        }
    }

    private String resolveBusinessKey(SaveTemplateCommand command) {
        String businessKey = trimToNull(command.getBusinessKey());
        if (StringUtils.hasText(businessKey)) {
            return businessKey;
        }
        return command.getTemplateCode().trim();
    }

    private String requireTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, TemplateCode.TEMPLATE_VALIDATION_ERROR, "机构上下文不能为空");
        return tenantId;
    }

    private TemplateDetailVO toDetailVO(TemplateEntity entity) {
        TemplateDetailVO vo = new TemplateDetailVO();
        copyTemplate(entity, vo);
        vo.setDraftContent(entity.getDraftContent());
        vo.setDraftSourceFileId(entity.getDraftSourceFileId());
        vo.setDraftVariables(parseVariables(entity.getDraftVariableSchema()).stream().map(this::toVariableVO).toList());
        return vo;
    }

    @Override
    protected TemplateVO toVO(TemplateEntity entity) {
        TemplateVO vo = new TemplateVO();
        copyTemplate(entity, vo);
        return vo;
    }

    private void copyTemplate(TemplateEntity entity, TemplateVO vo) {
        vo.setId(entity.getId());
        vo.setTenantId(Long.valueOf(entity.getTenantId()));
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setTemplateName(entity.getTemplateName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setDomainCode(entity.getDomainCode());
        vo.setBusinessGroup(entity.getBusinessGroup());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessKey(entity.getBusinessKey());
        vo.setSourceFormat(entity.getSourceFormat());
        vo.setStatus(entity.getStatus());
        vo.setCurrentVersionNo(entity.getCurrentVersionNo());
        vo.setPublishedVersionNo(entity.getCurrentVersionNo());
        vo.setHasUnpublishedChanges(Objects.equals(entity.getHasUnpublishedChanges(), 1));
        vo.setDraftSourceFormat(entity.getDraftSourceFormat());
        if (Objects.equals(entity.getHasUnpublishedChanges(), 1)) {
            vo.setUnpublishedChangeReasons(List.of("模板内容"));
        } else {
            vo.setUnpublishedChangeReasons(List.of());
        }
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedAt());
        vo.setUpdatedTime(entity.getUpdatedAt());
    }

    private TemplateVersionVO toVersionVO(TemplateVersionEntity entity) {
        TemplateVersionVO vo = new TemplateVersionVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setSourceFormat(entity.getSourceFormat());
        vo.setContent(entity.getContent());
        vo.setSourceFileId(entity.getSourceFileId());
        vo.setVariableSchema(entity.getVariableSchema());
        vo.setVariables(parseVariables(entity.getVariableSchema()).stream().map(this::toVariableVO).toList());
        vo.setCurrentPublished(entity.getCurrentPublished());
        vo.setVersionRemark(entity.getVersionRemark());
        vo.setCreatedTime(entity.getCreatedAt());
        return vo;
    }

    private TemplateRenderRecordVO toRenderRecordVO(TemplateRenderRecordEntity entity) {
        TemplateRenderRecordVO vo = new TemplateRenderRecordVO();
        vo.setId(entity.getId());
        vo.setTenantId(Long.valueOf(entity.getTenantId()));
        vo.setTemplateId(entity.getTemplateId());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setVersionId(entity.getVersionId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setOutputFormat(entity.getOutputFormat());
        vo.setStatus(entity.getStatus());
        vo.setOutputFileId(entity.getOutputFileId());
        vo.setOutputContent(entity.getOutputContent());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setCreatedTime(entity.getCreatedAt());
        vo.setUpdatedTime(entity.getUpdatedAt());
        return vo;
    }

    private List<TemplateVariableCommand> parseVariables(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return TemplateJsonCodec.read(json, VARIABLE_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private TemplateVariableVO toVariableVO(TemplateVariableCommand source) {
        TemplateVariableVO target = new TemplateVariableVO();
        target.setName(source.getName());
        target.setLabel(source.getLabel());
        target.setType(source.getType());
        target.setRequired(source.getRequired());
        target.setExample(source.getExample());
        target.setDescription(source.getDescription());
        target.setChildren(source.getChildren().stream().map(this::toVariableVO).toList());
        return target;
    }

    private Map<String, Object> renderVariables(TemplateRenderCommand command) {
        TemplateJsonRequest variables = command.getVariables();
        if (variables == null) {
            return Map.of();
        }
        return variables.toMap();
    }

    private TemplateSourceFormat parseSourceFormat(String sourceFormat) {
        if (!StringUtils.hasText(sourceFormat)) {
            return null;
        }
        TemplateSourceFormat resolved = Arrays.stream(TemplateSourceFormat.values())
                .filter(item -> item.name().equalsIgnoreCase(sourceFormat.trim()))
                .findFirst()
                .orElse(null);
        Require.notNull(resolved, TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED);
        return resolved;
    }

    private String toJson(Object value) {
        try {
            Object resolved = value;
            if (resolved == null) {
                resolved = List.of();
            }
            return TemplateJsonCodec.write(resolved);
        } catch (Exception e) {
            Require.isTrue(false, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板数据序列化失败");
            return "[]";
        }
    }

    private String toVariableJson(Map<String, Object> variables) {
        try {
            Map<String, Object> resolved = variables;
            if (resolved == null) {
                resolved = Map.of();
            }
            return TemplateJsonCodec.write(resolved);
        } catch (Exception e) {
            Require.isTrue(false, TemplateCode.TEMPLATE_VALIDATION_ERROR, "模板变量序列化失败");
            return "{}";
        }
    }

    private String trimToNull(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private TemplateDomainInfo validateDomain(String domainCode) {
        Require.notBlank(domainCode, TemplateCode.TEMPLATE_VALIDATION_ERROR, "业务域不能为空");
        TemplateDomainInfo domain = domainProvider.findByCode(domainCode.trim());
        Require.notNull(domain, TemplateCode.TEMPLATE_VALIDATION_ERROR, "业务域不存在");
        Require.isTrue(Integer.valueOf(1).equals(domain.status()),
                TemplateCode.TEMPLATE_VALIDATION_ERROR, "业务域已停用");
        return domain;
    }

    @Override
    protected Class<TemplateEntity> entityType() {
        return TemplateEntity.class;
    }

    private record RenderContext(TemplateEntity template,
                                 TemplateVersionEntity version,
                                 TemplateRenderRecordEntity record,
                                 TemplateRenderCommand command) {
    }
}
