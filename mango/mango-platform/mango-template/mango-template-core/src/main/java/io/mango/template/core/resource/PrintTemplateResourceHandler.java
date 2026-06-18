package io.mango.template.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.template.api.enums.TemplateSourceFormat;
import io.mango.template.api.enums.TemplateStatus;
import io.mango.template.core.entity.Template;
import io.mango.template.core.entity.TemplateCategory;
import io.mango.template.core.entity.TemplateRenderRecord;
import io.mango.template.core.entity.TemplateVersion;
import io.mango.template.core.mapper.TemplateCategoryMapper;
import io.mango.template.core.mapper.TemplateMapper;
import io.mango.template.core.mapper.TemplateRenderRecordMapper;
import io.mango.template.core.mapper.TemplateVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 打印模板资源处理器。
 */
@Component
@RequiredArgsConstructor
public class PrintTemplateResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "template";
    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int DEFAULT_STATUS = 1;
    private static final int DISABLED = 0;

    private final TemplateMapper templateMapper;
    private final TemplateCategoryMapper categoryMapper;
    private final TemplateVersionMapper versionMapper;
    private final TemplateRenderRecordMapper renderRecordMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.PRINT_TEMPLATE;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("templateCode")
                .requiredField("templateName")
                .requiredField("sourceFormat")
                .fieldDescription("templateId", "模板稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("versionId", "模板版本稳定 ID，可选；不填时由数据库生成。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("templateCode", "模板编码，同一租户唯一。")
                .fieldDescription("templateName", "模板名称。")
                .fieldDescription("categoryCode", "模板分类编码，可选。")
                .fieldDescription("categoryName", "模板分类名称；填写 categoryCode 时可同步创建或更新分类。")
                .fieldDescription("domainCode", "业务域编码。")
                .fieldDescription("businessGroup", "业务组编码。")
                .fieldDescription("businessType", "业务类型。")
                .fieldDescription("businessKey", "业务 Key，同一租户唯一；不填时默认使用模板编码。")
                .fieldDescription("sourceFormat", "模板源格式：TEXT、HTML、DOCX、XLSX。")
                .fieldDescription("content", "文本或 HTML 模板内容。")
                .fieldDescription("sourceFileId", "DOCX/XLSX 等文件模板源文件 ID。")
                .fieldDescription("variableSchema", "变量定义 JSON。")
                .fieldDescription("versionNo", "发布版本号，默认使用资源 version。")
                .fieldDescription("status", "状态：1 启用，0 停用。")
                .fieldDescription("remark", "模板备注。")
                .fieldDescription("versionRemark", "版本备注。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        PrintTemplatePayload payload = PrintTemplatePayload.from(resource);
        ensureCategory(payload);
        Template template = findTemplate(payload.tenantId(), payload.templateCode());
        if (template == null) {
            template = new Template();
            template.setId(payload.templateId());
            template.setTenantId(payload.tenantId());
            template.setTemplateCode(payload.templateCode());
            applyTemplate(template, payload);
            templateMapper.insert(template);
        } else {
            applyTemplate(template, payload);
            templateMapper.updateById(template);
        }
        upsertVersion(template, payload);
        return ResourceSyncResult.of(template.getId(), TARGET_TABLE,
                "Print template synced: " + payload.templateCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Template template = resolveTemplate(resource);
        if (template == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Print template not found");
        }
        template.setStatus(DISABLED);
        template.setUpdatedTime(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(template);
        return ResourceSyncResult.of(template.getId(), TARGET_TABLE,
                "Print template disabled: " + template.getTemplateCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        Template template = resolveTemplate(resource);
        if (template == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Print template not found");
        }
        renderRecordMapper.delete(new LambdaQueryWrapper<TemplateRenderRecord>()
                .eq(TemplateRenderRecord::getTemplateId, template.getId()));
        versionMapper.delete(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId()));
        templateMapper.deleteById(template.getId());
        return ResourceSyncResult.of(template.getId(), TARGET_TABLE,
                "Print template deleted: " + template.getTemplateCode());
    }

    private void applyTemplate(Template template, PrintTemplatePayload payload) {
        LocalDateTime now = LocalDateTime.now();
        template.setTenantId(payload.tenantId());
        template.setTemplateCode(payload.templateCode());
        template.setTemplateName(payload.templateName());
        template.setCategoryCode(payload.categoryCode());
        template.setCategoryName(payload.categoryName());
        template.setDomainCode(payload.domainCode());
        template.setBusinessGroup(payload.businessGroup());
        template.setBusinessType(payload.businessType());
        template.setBusinessKey(payload.businessKey());
        template.setSourceFormat(payload.sourceFormat());
        template.setStatus(payload.status());
        template.setCurrentVersionNo(payload.versionNo());
        template.setDraftSourceFormat(payload.sourceFormat());
        template.setDraftContent(payload.content());
        template.setDraftSourceFileId(payload.sourceFileId());
        template.setDraftVariableSchema(payload.variableSchema());
        template.setHasUnpublishedChanges(0);
        template.setRemark(payload.remark());
        if (template.getCreatedTime() == null) {
            template.setCreatedTime(now);
        }
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(now);
        }
        template.setUpdatedTime(now);
        template.setUpdatedAt(now);
    }

    private void ensureCategory(PrintTemplatePayload payload) {
        if (!StringUtils.hasText(payload.categoryCode())) {
            return;
        }
        TemplateCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<TemplateCategory>()
                .eq(TemplateCategory::getTenantId, payload.tenantId())
                .eq(TemplateCategory::getCategoryCode, payload.categoryCode())
                .last("limit 1"));
        if (category == null) {
            category = new TemplateCategory();
            category.setId(payload.categoryId());
            category.setTenantId(payload.tenantId());
            category.setCategoryCode(payload.categoryCode());
            category.setCategoryName(defaultText(payload.categoryName(), payload.categoryCode()));
            category.setSort(payload.categorySort());
            category.setStatus(DEFAULT_STATUS);
            category.setRemark(payload.categoryRemark());
            LocalDateTime now = LocalDateTime.now();
            category.setCreatedTime(now);
            category.setCreatedAt(now);
            category.setUpdatedTime(now);
            category.setUpdatedAt(now);
            categoryMapper.insert(category);
            return;
        }
        category.setCategoryName(defaultText(payload.categoryName(), category.getCategoryName()));
        category.setSort(payload.categorySort());
        category.setRemark(payload.categoryRemark());
        category.setUpdatedTime(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    private void upsertVersion(Template template, PrintTemplatePayload payload) {
        versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId())
                .set(TemplateVersion::getCurrentPublished, 0));
        TemplateVersion version = null;
        if (payload.versionId() != null) {
            version = versionMapper.selectById(payload.versionId());
            if (version != null && !template.getId().equals(version.getTemplateId())) {
                throw new IllegalStateException("PRINT_TEMPLATE versionId belongs to another template");
            }
        }
        if (version == null) {
            version = versionMapper.selectOne(new LambdaQueryWrapper<TemplateVersion>()
                    .eq(TemplateVersion::getTemplateId, template.getId())
                    .eq(TemplateVersion::getVersionNo, payload.versionNo())
                    .last("limit 1"));
        }
        if (version == null) {
            version = new TemplateVersion();
            version.setId(payload.versionId());
            version.setTenantId(payload.tenantId());
            version.setTemplateId(template.getId());
            version.setVersionNo(payload.versionNo());
            applyVersion(version, payload);
            versionMapper.insert(version);
            return;
        }
        applyVersion(version, payload);
        versionMapper.updateById(version);
    }

    private void applyVersion(TemplateVersion version, PrintTemplatePayload payload) {
        LocalDateTime now = LocalDateTime.now();
        version.setTenantId(payload.tenantId());
        version.setVersionNo(payload.versionNo());
        version.setSourceFormat(payload.sourceFormat());
        version.setContent(payload.content());
        version.setSourceFileId(payload.sourceFileId());
        version.setVariableSchema(payload.variableSchema());
        version.setCurrentPublished(1);
        version.setVersionRemark(payload.versionRemark());
        if (version.getCreatedTime() == null) {
            version.setCreatedTime(now);
        }
        if (version.getCreatedAt() == null) {
            version.setCreatedAt(now);
        }
        version.setUpdatedTime(now);
        version.setUpdatedAt(now);
    }

    private Template resolveTemplate(ResourceDeclaration resource) {
        Long tenantId = fieldLong(resource, "tenantId", false, DEFAULT_TENANT_ID);
        String templateCode = fieldText(resource, "templateCode", false);
        if (StringUtils.hasText(templateCode)) {
            Template template = findTemplate(tenantId, templateCode.trim());
            if (template != null) {
                return template;
            }
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId != null) {
            return templateMapper.selectById(targetId);
        }
        Long templateId = fieldLong(resource, "templateId", false, null);
        return templateId == null ? null : templateMapper.selectById(templateId);
    }

    private Template findTemplate(Long tenantId, String templateCode) {
        return templateMapper.selectOne(new LambdaQueryWrapper<Template>()
                .eq(Template::getTenantId, tenantId)
                .eq(Template::getTemplateCode, templateCode)
                .last("limit 1"));
    }

    private record PrintTemplatePayload(Long templateId, Long versionId, Long categoryId, Long tenantId,
                                        String templateCode, String templateName, String categoryCode,
                                        String categoryName, Integer categorySort, String categoryRemark,
                                        String domainCode, String businessGroup, String businessType,
                                        String businessKey, String sourceFormat, String content, Long sourceFileId,
                                        String variableSchema, Integer versionNo, Integer status, String remark,
                                        String versionRemark) {

        private static PrintTemplatePayload from(ResourceDeclaration resource) {
            String templateCode = requiredText(fieldValue(resource, "templateCode", true),
                    "PRINT_TEMPLATE templateCode is required").trim();
            String sourceFormat = parseSourceFormat(requiredText(fieldValue(resource, "sourceFormat", true),
                    "PRINT_TEMPLATE sourceFormat is required"));
            return new PrintTemplatePayload(
                    fieldLong(resource, "templateId", false, Long.valueOf(resource.getId())),
                    fieldLong(resource, "versionId", false, null),
                    fieldLong(resource, "categoryId", false, null),
                    fieldLong(resource, "tenantId", false, DEFAULT_TENANT_ID),
                    templateCode,
                    requiredText(fieldValue(resource, "templateName", true),
                            "PRINT_TEMPLATE templateName is required").trim(),
                    fieldText(resource, "categoryCode", false),
                    fieldText(resource, "categoryName", false),
                    fieldInt(resource, "categorySort", false, 0),
                    fieldText(resource, "categoryRemark", false),
                    fieldText(resource, "domainCode", false),
                    fieldText(resource, "businessGroup", false),
                    fieldText(resource, "businessType", false),
                    defaultText(fieldText(resource, "businessKey", false), templateCode),
                    sourceFormat,
                    fieldText(resource, "content", false),
                    fieldLong(resource, "sourceFileId", false, null),
                    fieldText(resource, "variableSchema", false),
                    fieldInt(resource, "versionNo", false, resource.getVersion() == null ? 1 : resource.getVersion()),
                    normalizeStatus(fieldInt(resource, "status", false, TemplateStatus.ENABLED.value())),
                    fieldText(resource, "remark", false),
                    fieldText(resource, "versionRemark", false)
            );
        }
    }

    private static String parseSourceFormat(String value) {
        return TemplateSourceFormat.valueOf(value.trim().toUpperCase()).name();
    }

    private static Integer normalizeStatus(Integer status) {
        if (status == null) {
            return TemplateStatus.ENABLED.value();
        }
        if (status != TemplateStatus.ENABLED.value() && status != TemplateStatus.DISABLED.value()) {
            throw new IllegalStateException("PRINT_TEMPLATE status is invalid: " + status);
        }
        return status;
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("PRINT_TEMPLATE field is required: " + name);
        }
        return value;
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Integer fieldInt(ResourceDeclaration resource, String name, boolean required,
                                    Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    private static String requiredText(Object value, String message) {
        String text = toText(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(message);
        }
        return text;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("PRINT_TEMPLATE long value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer toInt(Object value, boolean required, Integer defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("PRINT_TEMPLATE int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
