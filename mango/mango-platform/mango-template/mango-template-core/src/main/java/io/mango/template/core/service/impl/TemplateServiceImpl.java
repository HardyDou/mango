package io.mango.template.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.domain.api.DomainApi;
import io.mango.domain.api.vo.DomainVO;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.template.api.TemplateCode;
import io.mango.template.api.command.*;
import io.mango.template.api.enums.*;
import io.mango.template.api.query.TemplatePageQuery;
import io.mango.template.api.query.TemplateRenderRecordPageQuery;
import io.mango.template.api.vo.*;
import io.mango.template.core.entity.Template;
import io.mango.template.core.entity.TemplateRenderRecord;
import io.mango.template.core.entity.TemplateVersion;
import io.mango.template.core.mapper.TemplateMapper;
import io.mango.template.core.mapper.TemplateRenderRecordMapper;
import io.mango.template.core.mapper.TemplateVersionMapper;
import io.mango.template.core.render.TemplateRenderManager;
import io.mango.template.core.render.TemplateRenderOutput;
import io.mango.template.core.render.TemplateRenderPayload;
import io.mango.template.core.service.ITemplateFileStore;
import io.mango.template.core.service.ITemplateService;
import io.mango.template.core.service.TemplateStoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
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
public class TemplateServiceImpl implements ITemplateService {

    private static final TypeReference<List<TemplateVariableDefinition>> VARIABLE_LIST_TYPE = new TypeReference<>() {
    };
    private static final String VARIABLE_PATH_SEPARATOR = ".";
    private static final String VARIABLE_PATH_SEPARATOR_REGEX = "\\.";

    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper versionMapper;
    private final TemplateRenderRecordMapper renderRecordMapper;
    private final TemplateRenderManager renderManager;
    private final ITemplateFileStore fileStore;
    private final ObjectMapper objectMapper;
    private final Executor templateRenderExecutor;
    private final DomainApi domainApi;

    @Override
    public R<PageResult<TemplateVO>> page(TemplatePageQuery query) {
        TemplatePageQuery resolved = query == null ? new TemplatePageQuery() : query;
        IPage<Template> page = templateMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                templateWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Override
    public R<TemplateDetailVO> detail(Long id) {
        Template template = selectTemplate(id);
        TemplateDetailVO vo = toDetailVO(template);
        List<TemplateVersion> versions = versionMapper.selectList(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId())
                .orderByDesc(TemplateVersion::getVersionNo));
        vo.setVersions(versions.stream().map(this::toVersionVO).collect(Collectors.toList()));
        return R.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> create(SaveTemplateCommand command) {
        validateSave(command, false);
        Long tenantId = requireTenantId();
        Long userId = MangoContextHolder.userId();
        Require.isNull(templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTenantId, tenantId)
                .eq(Template::getTemplateCode, command.getTemplateCode())
                .last("LIMIT 1")), TemplateCode.TEMPLATE_CODE_DUPLICATED);
        validateBusinessKeyUnique(tenantId, resolveBusinessKey(command), null);
        Template entity = new Template();
        entity.setTenantId(tenantId);
        entity.setStatus(TemplateStatus.ENABLED.value());
        entity.setCurrentVersionNo(0);
        entity.setHasUnpublishedChanges(0);
        applyTemplate(entity, command);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedTime(now);
        entity.setUpdatedTime(now);
        templateMapper.insert(entity);
        return R.ok(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> update(SaveTemplateCommand command) {
        validateSave(command, true);
        Template entity = selectTemplate(command.getId());
        validateBusinessKeyUnique(entity.getTenantId(), resolveBusinessKey(command), entity.getId());
        applyTemplate(entity, command);
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        return R.ok(templateMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(Long id) {
        Template entity = selectTemplate(id);
        renderRecordMapper.delete(new LambdaQueryWrapper<TemplateRenderRecord>()
                .eq(TemplateRenderRecord::getTemplateId, entity.getId()));
        versionMapper.delete(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, entity.getId()));
        return R.ok(templateMapper.deleteById(entity.getId()) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateStatus(UpdateTemplateStatusCommand command) {
        Require.notNull(command, "模板状态命令不能为空");
        Template entity = selectTemplate(command.getId());
        entity.setStatus(command.getStatus());
        entity.setUpdatedBy(MangoContextHolder.userId());
        entity.setUpdatedTime(LocalDateTime.now());
        return R.ok(templateMapper.updateById(entity) > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> publishVersion(PublishTemplateVersionCommand command) {
        Require.notNull(command, "模板版本命令不能为空");
        Template template = selectTemplate(command.getTemplateId());
        validateVersionSource(command);
        Integer nextVersion = nextVersionNo(template.getId());
        versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId())
                .set(TemplateVersion::getCurrentPublished, 0));
        TemplateVersion version = new TemplateVersion();
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
        version.setCreatedTime(now);
        version.setUpdatedTime(now);
        versionMapper.insert(version);
        template.setSourceFormat(command.getSourceFormat().name());
        template.setCurrentVersionNo(nextVersion);
        template.setDraftSourceFormat(command.getSourceFormat().name());
        template.setDraftContent(trimToNull(command.getContent()));
        template.setDraftSourceFileId(command.getSourceFileId());
        template.setDraftVariableSchema(toJson(command.getVariables()));
        template.setHasUnpublishedChanges(0);
        template.setUpdatedBy(MangoContextHolder.userId());
        template.setUpdatedTime(now);
        templateMapper.updateById(template);
        return R.ok(version.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> activateVersion(ActivateTemplateVersionCommand command) {
        Require.notNull(command, "启用模板版本命令不能为空");
        Template template = selectTemplate(command.getTemplateId());
        Require.notNull(command.getVersionNo(), "模板版本号不能为空");
        TemplateVersion version = versionMapper.selectOne(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId())
                .eq(TemplateVersion::getVersionNo, command.getVersionNo())
                .last("LIMIT 1"));
        Require.notNull(version, TemplateCode.TEMPLATE_VERSION_NOT_FOUND);
        versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId())
                .set(TemplateVersion::getCurrentPublished, 0));
        version.setCurrentPublished(1);
        version.setUpdatedBy(MangoContextHolder.userId());
        version.setUpdatedTime(LocalDateTime.now());
        versionMapper.updateById(version);
        template.setSourceFormat(version.getSourceFormat());
        template.setCurrentVersionNo(version.getVersionNo());
        template.setDraftSourceFormat(version.getSourceFormat());
        template.setDraftContent(version.getContent());
        template.setDraftSourceFileId(version.getSourceFileId());
        template.setDraftVariableSchema(version.getVariableSchema());
        template.setHasUnpublishedChanges(0);
        template.setUpdatedBy(MangoContextHolder.userId());
        template.setUpdatedTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return R.ok(true);
    }

    @Override
    public R<List<String>> extractVariables(ExtractTemplateVariablesCommand command) {
        Require.notNull(command, "变量提取命令不能为空");
        Require.notNull(command.getSourceFormat(), "模板源格式不能为空");
        TemplateRenderPayload payload = payload(command.getSourceFormat(), TemplateOutputFormat.TEXT,
                command.getContent(), command.getSourceFileId(), Map.of());
        return R.ok(renderManager.extractVariables(payload));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TemplateRenderResultVO> render(TemplateRenderCommand command) {
        RenderContext context = prepareRender(command, TemplateRenderStatus.RUNNING);
        try {
            TemplateRenderResultVO result = doRender(context);
            markSuccess(context.record(), result);
            result.setRecordId(context.record().getId());
            result.setStatus(TemplateRenderStatus.SUCCESS.name());
            return R.ok(result);
        } catch (Exception e) {
            markFailed(context.record(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TemplateRenderResultVO> renderAsync(TemplateRenderCommand command) {
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
        return R.ok(result);
    }

    public void executeAsync(Long recordId) {
        TemplateRenderRecord record = renderRecordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        try {
            record.setStatus(TemplateRenderStatus.RUNNING.name());
            record.setUpdatedTime(LocalDateTime.now());
            renderRecordMapper.updateById(record);
            Template template = templateMapper.selectById(record.getTemplateId());
            TemplateVersion version = versionMapper.selectById(record.getVersionId());
            Map<String, Object> variables = objectMapper.readValue(record.getVariablePayload(), new TypeReference<>() {
            });
            TemplateRenderCommand command = new TemplateRenderCommand();
            command.setTemplateCode(record.getTemplateCode());
            command.setVersionNo(record.getVersionNo());
            command.setOutputFormat(TemplateOutputFormat.valueOf(record.getOutputFormat()));
            command.setVariables(variables);
            command.setBizType(record.getBizType());
            command.setBizId(record.getBizId());
            TemplateRenderResultVO result = renderLoaded(template, version, command);
            markSuccess(record, result);
        } catch (Exception e) {
            markFailed(record, e);
        }
    }

    @Override
    public R<TemplateRenderRecordVO> renderRecord(Long id) {
        Require.notNull(id, "渲染记录ID不能为空");
        TemplateRenderRecord record = renderRecordMapper.selectById(id);
        Require.notNull(record, TemplateCode.TEMPLATE_RENDER_RECORD_NOT_FOUND);
        return R.ok(toRenderRecordVO(record));
    }

    @Override
    public R<PageResult<TemplateRenderRecordVO>> renderRecordPage(TemplateRenderRecordPageQuery query) {
        TemplateRenderRecordPageQuery resolved = query == null ? new TemplateRenderRecordPageQuery() : query;
        IPage<TemplateRenderRecord> page = renderRecordMapper.selectPage(
                new Page<>(resolved.getPage(), resolved.getSize()),
                recordWrapper(resolved));
        return R.ok(PageResult.of(page.getRecords().stream().map(this::toRenderRecordVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize()));
    }

    private RenderContext prepareRender(TemplateRenderCommand command, TemplateRenderStatus initialStatus) {
        Require.notNull(command, "模板渲染命令不能为空");
        Require.notNull(command.getOutputFormat(), "输出格式不能为空");
        Template template = selectTemplateForRender(command);
        Require.isTrue(TemplateStatus.ENABLED.value() == template.getStatus(), TemplateCode.TEMPLATE_DISABLED);
        TemplateVersion version = selectVersion(template, command.getVersionNo());
        validateRequiredVariables(version, command.getVariables());
        TemplateRenderRecord record = new TemplateRenderRecord();
        record.setTenantId(template.getTenantId());
        record.setTemplateId(template.getId());
        record.setTemplateCode(template.getTemplateCode());
        record.setVersionId(version.getId());
        record.setVersionNo(version.getVersionNo());
        record.setOutputFormat(command.getOutputFormat().name());
        record.setStatus(initialStatus.name());
        record.setVariablePayload(toVariableJson(command.getVariables()));
        record.setBizType(trimToNull(command.getBizType()));
        record.setBizId(trimToNull(command.getBizId()));
        record.setCreatedBy(MangoContextHolder.userId());
        record.setUpdatedBy(MangoContextHolder.userId());
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedTime(now);
        record.setUpdatedTime(now);
        renderRecordMapper.insert(record);
        return new RenderContext(template, version, record, command);
    }

    private TemplateRenderResultVO doRender(RenderContext context) {
        return renderLoaded(context.template(), context.version(), context.command());
    }

    private TemplateRenderResultVO renderLoaded(Template template, TemplateVersion version, TemplateRenderCommand command) {
        TemplateRenderPayload payload = payload(TemplateSourceFormat.valueOf(version.getSourceFormat()),
                command.getOutputFormat(), version.getContent(), version.getSourceFileId(), command.getVariables(),
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
                                          List<TemplateVariableDefinition> variableDefinitions) {
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
        } catch (Exception e) {
            throw new BizException(TemplateCode.TEMPLATE_FILE_NOT_FOUND.getCode(), "读取模板文件失败");
        }
    }

    private void markSuccess(TemplateRenderRecord record, TemplateRenderResultVO result) {
        record.setStatus(TemplateRenderStatus.SUCCESS.name());
        record.setOutputFileId(result.getFileId());
        record.setOutputContent(result.getContent());
        record.setErrorMessage(null);
        record.setUpdatedTime(LocalDateTime.now());
        renderRecordMapper.updateById(record);
    }

    private void markFailed(TemplateRenderRecord record, Exception e) {
        record.setStatus(TemplateRenderStatus.FAILED.name());
        record.setErrorMessage(e.getMessage());
        record.setUpdatedTime(LocalDateTime.now());
        renderRecordMapper.updateById(record);
    }

    private void validateSave(SaveTemplateCommand command, boolean update) {
        Require.notNull(command, "模板保存命令不能为空");
        if (update) {
            Require.notNull(command.getId(), "模板ID不能为空");
        }
        Require.notBlank(command.getTemplateCode(), "模板编码不能为空");
        Require.notBlank(command.getTemplateName(), "模板名称不能为空");
        validateDomain(command.getDomainCode());
    }

    private void validateVersionSource(PublishTemplateVersionCommand command) {
        Require.notNull(command.getSourceFormat(), "内容稿源格式不能为空");
        TemplateSourceFormat sourceFormat = command.getSourceFormat();
        if (sourceFormat == TemplateSourceFormat.TEXT || sourceFormat == TemplateSourceFormat.HTML) {
            Require.notBlank(command.getContent(), "文本/HTML 模板内容不能为空");
            return;
        }
        Require.notNull(command.getSourceFileId(), "文档模板文件不能为空");
    }

    private void validateRequiredVariables(TemplateVersion version, Map<String, Object> variables) {
        validateVariables(parseVariables(version.getVariableSchema()), variables == null ? Map.of() : variables, "");
    }

    private void validateVariables(List<TemplateVariableDefinition> definitions,
                                   Map<String, Object> variables,
                                   String parentPath) {
        validateVariables(definitions, variables, parentPath, parentPath);
    }

    private void validateVariables(List<TemplateVariableDefinition> definitions,
                                   Map<String, Object> variables,
                                   String parentPath,
                                   String displayParentPath) {
        for (TemplateVariableDefinition definition : definitions) {
            String resolvePath = variablePath(parentPath, definition.getName());
            String displayPath = variablePath(displayParentPath, definition.getName());
            if (!StringUtils.hasText(resolvePath)) {
                continue;
            }
            Object value = resolveVariable(resolvePath, variables);
            if (!Boolean.FALSE.equals(definition.getRequired()) && value == null) {
                throw new BizException(TemplateCode.TEMPLATE_VARIABLE_MISSING.getCode(), "缺少模板变量：" + displayPath);
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

    private void validateArrayChildren(TemplateVariableDefinition definition, String path, Object value) {
        Iterable<?> items = toIterable(value);
        int index = 0;
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new BizException(TemplateCode.TEMPLATE_VARIABLE_MISSING.getCode(),
                        "模板变量类型不匹配：" + path + "[" + index + "]，期望 OBJECT");
            }
            validateVariables(definition.getChildren(), castMap(map), "", path + "[" + index + "]");
            index++;
        }
    }

    private boolean isArrayDefinition(TemplateVariableDefinition definition) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private void validateVariableType(TemplateVariableDefinition definition, String path, Object value) {
        if (value == null) {
            return;
        }
        String type = Optional.ofNullable(definition.getType()).orElse("STRING").trim().toUpperCase(Locale.ROOT);
        boolean valid = switch (type) {
            case "NUMBER" -> value instanceof Number || parseableNumber(value);
            case "BOOLEAN" -> value instanceof Boolean || "true".equalsIgnoreCase(String.valueOf(value))
                    || "false".equalsIgnoreCase(String.valueOf(value));
            case "OBJECT" -> value instanceof Map<?, ?>;
            case "ARRAY" -> value instanceof Collection<?> || value.getClass().isArray();
            case "DATE", "STRING" -> true;
            default -> true;
        };
        if (!valid) {
            throw new BizException(TemplateCode.TEMPLATE_VARIABLE_MISSING.getCode(),
                    "模板变量类型不匹配：" + path + "，期望 " + type);
        }
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
        TemplateVersion latest = versionMapper.selectOne(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, templateId)
                .orderByDesc(TemplateVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getVersionNo() + 1;
    }

    private Template selectTemplate(Long id) {
        Require.notNull(id, "模板ID不能为空");
        Template template = templateMapper.selectById(id);
        Require.notNull(template, TemplateCode.TEMPLATE_NOT_FOUND);
        Require.isTrue(Objects.equals(template.getTenantId(), requireTenantId()), TemplateCode.TEMPLATE_NOT_FOUND);
        return template;
    }

    private Template selectTemplateByCode(String templateCode) {
        Template template = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTemplateCode, templateCode)
                .eq(Template::getTenantId, requireTenantId())
                .last("LIMIT 1"));
        Require.notNull(template, TemplateCode.TEMPLATE_NOT_FOUND);
        return template;
    }

    private Template selectTemplateForRender(TemplateRenderCommand command) {
        String templateCode = trimToNull(command.getTemplateCode());
        String businessKey = trimToNull(command.getBusinessKey());
        Require.isTrue(StringUtils.hasText(templateCode) || StringUtils.hasText(businessKey), "模板编码不能为空");
        Require.isFalse(StringUtils.hasText(templateCode) && StringUtils.hasText(businessKey), "模板编码和兼容业务KEY只能传一个");
        if (StringUtils.hasText(templateCode)) {
            return selectTemplateByCode(templateCode);
        }
        Template template = templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTenantId, requireTenantId())
                .eq(Template::getBusinessKey, businessKey)
                .last("LIMIT 1"));
        Require.notNull(template, TemplateCode.TEMPLATE_NOT_FOUND);
        return template;
    }

    private TemplateVersion selectVersion(Template template, Integer versionNo) {
        LambdaQueryWrapper<TemplateVersion> wrapper = new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId());
        if (versionNo == null) {
            wrapper.eq(TemplateVersion::getCurrentPublished, 1);
        } else {
            wrapper.eq(TemplateVersion::getVersionNo, versionNo);
        }
        TemplateVersion version = versionMapper.selectOne(wrapper.last("LIMIT 1"));
        Require.notNull(version, TemplateCode.TEMPLATE_VERSION_NOT_FOUND);
        return version;
    }

    private LambdaQueryWrapper<Template> templateWrapper(TemplatePageQuery query) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Template::getTenantId, requireTenantId());
        String keyword = trimToNull(query.getKeyword());
        wrapper.and(StringUtils.hasText(keyword), item -> item
                .like(Template::getTemplateCode, keyword)
                .or()
                .like(Template::getTemplateName, keyword));
        wrapper.eq(StringUtils.hasText(query.getCategoryCode()), Template::getCategoryCode, query.getCategoryCode());
        wrapper.eq(StringUtils.hasText(query.getDomainCode()), Template::getDomainCode, trimToNull(query.getDomainCode()));
        wrapper.eq(StringUtils.hasText(query.getBusinessKey()), Template::getBusinessKey, query.getBusinessKey());
        wrapper.eq(StringUtils.hasText(query.getSourceFormat()), Template::getSourceFormat, query.getSourceFormat());
        wrapper.eq(query.getStatus() != null, Template::getStatus, query.getStatus());
        wrapper.orderByDesc(Template::getId);
        return wrapper;
    }

    private void validateBusinessKeyUnique(Long tenantId, String businessKey, Long currentId) {
        if (!StringUtils.hasText(businessKey)) {
            return;
        }
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(Template::getTenantId, tenantId)
                .eq(Template::getBusinessKey, businessKey);
        if (currentId != null) {
            wrapper.ne(Template::getId, currentId);
        }
        Require.isNull(templateMapper.selectOne(wrapper.last("LIMIT 1")), TemplateCode.TEMPLATE_BUSINESS_KEY_DUPLICATED);
    }

    private LambdaQueryWrapper<TemplateRenderRecord> recordWrapper(TemplateRenderRecordPageQuery query) {
        LambdaQueryWrapper<TemplateRenderRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateRenderRecord::getTenantId, requireTenantId());
        wrapper.eq(StringUtils.hasText(query.getTemplateCode()), TemplateRenderRecord::getTemplateCode, query.getTemplateCode());
        wrapper.eq(StringUtils.hasText(query.getStatus()), TemplateRenderRecord::getStatus, query.getStatus());
        wrapper.eq(StringUtils.hasText(query.getBizType()), TemplateRenderRecord::getBizType, query.getBizType());
        wrapper.eq(StringUtils.hasText(query.getBizId()), TemplateRenderRecord::getBizId, query.getBizId());
        wrapper.orderByDesc(TemplateRenderRecord::getId);
        return wrapper;
    }

    private void applyTemplate(Template entity, SaveTemplateCommand command) {
        entity.setTemplateCode(command.getTemplateCode().trim());
        entity.setTemplateName(command.getTemplateName().trim());
        DomainVO domain = validateDomain(command.getDomainCode());
        entity.setDomainCode(domain.getDomainCode());
        entity.setCategoryCode(domain.getDomainCode());
        entity.setCategoryName(domain.getDomainName());
        entity.setBusinessGroup(trimToNull(command.getBusinessGroup()));
        entity.setBusinessType(trimToNull(command.getBusinessType()));
        entity.setBusinessKey(resolveBusinessKey(command));
        applyDraft(entity, command);
        entity.setRemark(trimToNull(command.getRemark()));
    }

    private void applyDraft(Template entity, SaveTemplateCommand command) {
        if (command.getSourceFormat() == null) {
            return;
        }
        entity.setDraftSourceFormat(command.getSourceFormat().name());
        entity.setDraftContent(trimToNull(command.getDraftContent()));
        entity.setDraftSourceFileId(command.getDraftSourceFileId());
        entity.setDraftVariableSchema(toJson(command.getDraftVariables()));
        entity.setHasUnpublishedChanges(hasDraftDifference(entity) ? 1 : 0);
    }

    private boolean hasDraftDifference(Template entity) {
        if (entity.getCurrentVersionNo() == null || entity.getCurrentVersionNo() <= 0) {
            return StringUtils.hasText(entity.getDraftSourceFormat())
                    || StringUtils.hasText(entity.getDraftContent())
                    || entity.getDraftSourceFileId() != null
                    || StringUtils.hasText(entity.getDraftVariableSchema());
        }
        TemplateVersion current = versionMapper.selectOne(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, entity.getId())
                .eq(TemplateVersion::getVersionNo, entity.getCurrentVersionNo())
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
            return objectMapper.writeValueAsString(objectMapper.readTree(json));
        } catch (Exception e) {
            return json;
        }
    }

    private String resolveBusinessKey(SaveTemplateCommand command) {
        String businessKey = trimToNull(command.getBusinessKey());
        return StringUtils.hasText(businessKey) ? businessKey : command.getTemplateCode().trim();
    }

    private Long requireTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "机构上下文不能为空");
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            throw new BizException("机构上下文非法");
        }
    }

    private TemplateDetailVO toDetailVO(Template entity) {
        TemplateDetailVO vo = new TemplateDetailVO();
        copyTemplate(entity, vo);
        vo.setDraftContent(entity.getDraftContent());
        vo.setDraftSourceFileId(entity.getDraftSourceFileId());
        vo.setDraftVariables(parseVariables(entity.getDraftVariableSchema()));
        return vo;
    }

    private TemplateVO toVO(Template entity) {
        TemplateVO vo = new TemplateVO();
        copyTemplate(entity, vo);
        return vo;
    }

    private void copyTemplate(Template entity, TemplateVO vo) {
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
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
        vo.setUnpublishedChangeReasons(Objects.equals(entity.getHasUnpublishedChanges(), 1) ? List.of("模板内容") : List.of());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
    }

    private TemplateVersionVO toVersionVO(TemplateVersion entity) {
        TemplateVersionVO vo = new TemplateVersionVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setSourceFormat(entity.getSourceFormat());
        vo.setContent(entity.getContent());
        vo.setSourceFileId(entity.getSourceFileId());
        vo.setVariableSchema(entity.getVariableSchema());
        vo.setVariables(parseVariables(entity.getVariableSchema()));
        vo.setCurrentPublished(entity.getCurrentPublished());
        vo.setVersionRemark(entity.getVersionRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        return vo;
    }

    private TemplateRenderRecordVO toRenderRecordVO(TemplateRenderRecord entity) {
        TemplateRenderRecordVO vo = new TemplateRenderRecordVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
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
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private List<TemplateVariableDefinition> parseVariables(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, VARIABLE_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            throw new BizException("模板数据序列化失败");
        }
    }

    private String toVariableJson(Map<String, Object> variables) {
        try {
            return objectMapper.writeValueAsString(variables == null ? Map.of() : variables);
        } catch (Exception e) {
            throw new BizException("模板变量序列化失败");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private DomainVO validateDomain(String domainCode) {
        Require.notBlank(domainCode, "业务域不能为空");
        R<DomainVO> response = domainApi.detailByCode(domainCode.trim());
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null, "业务域不存在");
        Require.isTrue(Integer.valueOf(1).equals(response.getData().getStatus()), "业务域已停用");
        return response.getData();
    }

    private record RenderContext(Template template,
                                 TemplateVersion version,
                                 TemplateRenderRecord record,
                                 TemplateRenderCommand command) {
    }
}
