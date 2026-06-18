package io.mango.notice.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessConfigVersionMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 通知业务消息模板资源处理器。
 */
@Component
@RequiredArgsConstructor
public class NoticeMessageTemplateResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "notice_business_channel_template";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final String DEFAULT_DOMAIN_CODE = "COMMON";

    private final NoticeBusinessTypeMapper businessTypeMapper;
    private final NoticeBusinessConfigVersionMapper configVersionMapper;
    private final NoticeBusinessChannelTemplateMapper channelTemplateMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.MESSAGE_TEMPLATE;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("bizType")
                .requiredField("bizName")
                .requiredField("channelType")
                .requiredField("templateName")
                .requiredField("titleTemplate")
                .requiredField("contentTemplate")
                .fieldDescription("businessTypeId", "通知业务类型稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("configVersionId", "通知业务配置版本稳定 ID。")
                .fieldDescription("channelTemplateId", "通知业务渠道模板稳定 ID。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("bizType", "通知业务类型编码，同一租户内唯一。")
                .fieldDescription("bizName", "通知业务类型名称。")
                .fieldDescription("bizGroup", "通知业务分组。")
                .fieldDescription("domainCode", "业务域编码。")
                .fieldDescription("description", "通知业务类型说明。")
                .fieldDescription("paramsSchema", "通知参数 JSON Schema。")
                .fieldDescription("defaultPriority", "默认优先级：LOW、NORMAL、HIGH、URGENT。")
                .fieldDescription("idempotentStrategy", "幂等策略。")
                .fieldDescription("version", "模板版本号。")
                .fieldDescription("versionStatus", "版本状态：DRAFT、ACTIVE、HISTORY。")
                .fieldDescription("channelType", "渠道类型：SITE、SMS、EMAIL、WECHAT_OFFICIAL、WECOM、DINGTALK。")
                .fieldDescription("templateName", "渠道模板名称。")
                .fieldDescription("titleTemplate", "标题模板。")
                .fieldDescription("contentTemplate", "内容模板。")
                .fieldDescription("externalTemplateId", "三方渠道模板 ID。")
                .fieldDescription("variableMapping", "变量映射 JSON。")
                .fieldDescription("enabled", "是否启用，默认 true。")
                .fieldDescription("channelConfigId", "绑定渠道配置 ID，空表示 AUTO。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        TemplatePayload payload = TemplatePayload.from(resource);
        NoticeBusinessTypeEntity businessType = upsertBusinessType(payload);
        NoticeBusinessConfigVersionEntity configVersion = upsertConfigVersion(payload, businessType.getId());
        NoticeBusinessChannelTemplateEntity channelTemplate = upsertChannelTemplate(payload, businessType.getId());
        return ResourceSyncResult.of(channelTemplate.getId(), TARGET_TABLE,
                "Notice message template synced: " + payload.tenantId() + ":" + payload.bizType()
                        + ":" + payload.channelType() + ":" + payload.version()
                        + ", configVersionId=" + configVersion.getId());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        TemplatePayload payload = TemplatePayload.from(resource);
        NoticeBusinessTypeEntity businessType = findBusinessType(payload.tenantId(), payload.bizType());
        NoticeBusinessChannelTemplateEntity channelTemplate = findChannelTemplate(payload.tenantId(), payload.bizType(),
                payload.channelType(), payload.version());
        LocalDateTime now = LocalDateTime.now();
        Long targetId = null;
        if (channelTemplate != null) {
            channelTemplate.setEnabled(false);
            channelTemplate.setUpdatedAt(now);
            channelTemplateMapper.updateById(channelTemplate);
            targetId = channelTemplate.getId();
        }
        if (businessType != null && countEnabledChannelTemplates(payload.tenantId(), payload.bizType()) == 0) {
            businessType.setEnabled(false);
            businessType.setUpdatedAt(now);
            businessTypeMapper.updateById(businessType);
        }
        return ResourceSyncResult.of(targetId, TARGET_TABLE,
                "Notice message template disabled: " + payload.tenantId() + ":" + payload.bizType());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        TemplatePayload payload = TemplatePayload.from(resource);
        NoticeBusinessChannelTemplateEntity channelTemplate = findChannelTemplate(payload.tenantId(), payload.bizType(),
                payload.channelType(), payload.version());
        NoticeBusinessConfigVersionEntity configVersion = findConfigVersion(payload.tenantId(), payload.bizType(),
                payload.version());
        NoticeBusinessTypeEntity businessType = findBusinessType(payload.tenantId(), payload.bizType());
        Long targetId = channelTemplate == null ? null : channelTemplate.getId();
        if (channelTemplate != null) {
            channelTemplateMapper.deleteById(channelTemplate.getId());
        }
        if (configVersion != null
                && countChannelTemplates(payload.tenantId(), payload.bizType(), payload.version()) == 0) {
            configVersionMapper.deleteById(configVersion.getId());
        }
        if (businessType != null && countChannelTemplates(payload.tenantId(), payload.bizType()) == 0
                && countConfigVersions(payload.tenantId(), payload.bizType()) == 0) {
            businessTypeMapper.deleteById(businessType.getId());
        }
        return ResourceSyncResult.of(targetId, TARGET_TABLE,
                "Notice message template deleted: " + payload.tenantId() + ":" + payload.bizType());
    }

    private NoticeBusinessTypeEntity upsertBusinessType(TemplatePayload payload) {
        NoticeBusinessTypeEntity entity = findBusinessType(payload.tenantId(), payload.bizType());
        if (entity == null) {
            entity = new NoticeBusinessTypeEntity();
            entity.setId(payload.businessTypeId());
            entity.setTenantId(payload.tenantId());
            entity.setBizType(payload.bizType());
            applyBusinessType(entity, payload);
            businessTypeMapper.insert(entity);
        } else {
            applyBusinessType(entity, payload);
            businessTypeMapper.updateById(entity);
        }
        return entity;
    }

    private NoticeBusinessConfigVersionEntity upsertConfigVersion(TemplatePayload payload, Long businessTypeId) {
        NoticeBusinessConfigVersionEntity entity = findConfigVersion(payload.tenantId(), payload.bizType(), payload.version());
        if (entity == null) {
            entity = new NoticeBusinessConfigVersionEntity();
            entity.setId(payload.configVersionId());
            entity.setTenantId(payload.tenantId());
            entity.setBizType(payload.bizType());
            entity.setVersion(payload.version());
            applyConfigVersion(entity, payload, businessTypeId);
            configVersionMapper.insert(entity);
        } else {
            applyConfigVersion(entity, payload, businessTypeId);
            configVersionMapper.updateById(entity);
        }
        return entity;
    }

    private NoticeBusinessChannelTemplateEntity upsertChannelTemplate(TemplatePayload payload, Long businessTypeId) {
        NoticeBusinessChannelTemplateEntity entity = findChannelTemplate(payload.tenantId(), payload.bizType(),
                payload.channelType(), payload.version());
        if (entity == null) {
            entity = new NoticeBusinessChannelTemplateEntity();
            entity.setId(payload.channelTemplateResourceId());
            entity.setTenantId(payload.tenantId());
            entity.setBizType(payload.bizType());
            entity.setChannelType(payload.channelType());
            entity.setVersion(payload.version());
            applyChannelTemplate(entity, payload, businessTypeId);
            channelTemplateMapper.insert(entity);
        } else {
            applyChannelTemplate(entity, payload, businessTypeId);
            channelTemplateMapper.updateById(entity);
        }
        return entity;
    }

    private void applyBusinessType(NoticeBusinessTypeEntity entity, TemplatePayload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setBizName(payload.bizName());
        entity.setBizGroup(payload.bizGroup());
        entity.setDomainCode(defaultText(payload.domainCode(), DEFAULT_DOMAIN_CODE));
        entity.setDescription(payload.description());
        entity.setParamsSchema(payload.paramsSchema());
        entity.setEnabled(payload.enabled());
        entity.setDefaultPriority(payload.defaultPriority());
        entity.setIdempotentStrategy(payload.idempotentStrategy());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private void applyConfigVersion(NoticeBusinessConfigVersionEntity entity, TemplatePayload payload, Long businessTypeId) {
        LocalDateTime now = LocalDateTime.now();
        entity.setBusinessTypeId(businessTypeId);
        entity.setParamsSchema(payload.paramsSchema());
        entity.setDefaultPriority(payload.defaultPriority());
        entity.setIdempotentStrategy(payload.idempotentStrategy());
        entity.setVersionStatus(payload.versionStatus());
        entity.setPublishTime(now);
        entity.setPublishBy(payload.operatorId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private void applyChannelTemplate(NoticeBusinessChannelTemplateEntity entity, TemplatePayload payload,
                                      Long businessTypeId) {
        LocalDateTime now = LocalDateTime.now();
        entity.setBusinessTypeId(businessTypeId);
        entity.setTemplateName(payload.templateName());
        entity.setTitleTemplate(payload.titleTemplate());
        entity.setContentTemplate(payload.contentTemplate());
        entity.setChannelTemplateId(payload.externalTemplateId());
        entity.setVariableMapping(payload.variableMapping());
        entity.setVersionStatus(payload.versionStatus());
        entity.setEnabled(payload.enabled());
        entity.setChannelConfigId(payload.channelConfigId());
        entity.setPublishTime(now);
        entity.setPublishBy(payload.operatorId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private NoticeBusinessTypeEntity findBusinessType(String tenantId, String bizType) {
        return businessTypeMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
                .eq(NoticeBusinessTypeEntity::getTenantId, tenantId)
                .eq(NoticeBusinessTypeEntity::getBizType, bizType)
                .last("limit 1"));
    }

    private NoticeBusinessConfigVersionEntity findConfigVersion(String tenantId, String bizType, Integer version) {
        return configVersionMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
                .eq(NoticeBusinessConfigVersionEntity::getTenantId, tenantId)
                .eq(NoticeBusinessConfigVersionEntity::getBizType, bizType)
                .eq(NoticeBusinessConfigVersionEntity::getVersion, version)
                .last("limit 1"));
    }

    private NoticeBusinessChannelTemplateEntity findChannelTemplate(String tenantId, String bizType,
                                                                    NoticeChannelType channelType, Integer version) {
        return channelTemplateMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
                .eq(NoticeBusinessChannelTemplateEntity::getTenantId, tenantId)
                .eq(NoticeBusinessChannelTemplateEntity::getBizType, bizType)
                .eq(NoticeBusinessChannelTemplateEntity::getChannelType, channelType)
                .eq(NoticeBusinessChannelTemplateEntity::getVersion, version)
                .last("limit 1"));
    }

    private Long countEnabledChannelTemplates(String tenantId, String bizType) {
        return channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
                .eq(NoticeBusinessChannelTemplateEntity::getTenantId, tenantId)
                .eq(NoticeBusinessChannelTemplateEntity::getBizType, bizType)
                .eq(NoticeBusinessChannelTemplateEntity::getEnabled, true));
    }

    private Long countChannelTemplates(String tenantId, String bizType) {
        return channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
                .eq(NoticeBusinessChannelTemplateEntity::getTenantId, tenantId)
                .eq(NoticeBusinessChannelTemplateEntity::getBizType, bizType));
    }

    private Long countChannelTemplates(String tenantId, String bizType, Integer version) {
        return channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
                .eq(NoticeBusinessChannelTemplateEntity::getTenantId, tenantId)
                .eq(NoticeBusinessChannelTemplateEntity::getBizType, bizType)
                .eq(NoticeBusinessChannelTemplateEntity::getVersion, version));
    }

    private Long countConfigVersions(String tenantId, String bizType) {
        return configVersionMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
                .eq(NoticeBusinessConfigVersionEntity::getTenantId, tenantId)
                .eq(NoticeBusinessConfigVersionEntity::getBizType, bizType));
    }

    private record TemplatePayload(Long businessTypeId, Long configVersionId, Long channelTemplateResourceId,
                                   String tenantId, String bizType, String bizName, String bizGroup, String domainCode,
                                   String description, String paramsSchema, Boolean enabled,
                                   NoticePriority defaultPriority, String idempotentStrategy, Integer version,
                                   NoticeTemplateVersionStatus versionStatus, NoticeChannelType channelType,
                                   String templateName, String titleTemplate, String contentTemplate,
                                   String externalTemplateId, String variableMapping, Long channelConfigId,
                                   Long operatorId) {

        private static TemplatePayload from(ResourceDeclaration resource) {
            String id = resource.getId();
            return new TemplatePayload(
                    fieldLong(resource, "businessTypeId", false, Long.valueOf(id)),
                    fieldLong(resource, "configVersionId", true, null),
                    fieldLong(resource, "channelTemplateId", true, null),
                    defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID),
                    requiredText(fieldValue(resource, "bizType", true), "MESSAGE_TEMPLATE bizType is required").trim(),
                    requiredText(fieldValue(resource, "bizName", true), "MESSAGE_TEMPLATE bizName is required").trim(),
                    fieldText(resource, "bizGroup", false),
                    fieldText(resource, "domainCode", false),
                    fieldText(resource, "description", false),
                    fieldText(resource, "paramsSchema", false),
                    fieldBoolean(resource, "enabled", false, true),
                    parseEnum(NoticePriority.class, fieldText(resource, "defaultPriority", false), NoticePriority.NORMAL),
                    fieldText(resource, "idempotentStrategy", false),
                    fieldInt(resource, "version", false, resource.getVersion() == null ? 1 : resource.getVersion()),
                    parseEnum(NoticeTemplateVersionStatus.class, fieldText(resource, "versionStatus", false),
                            NoticeTemplateVersionStatus.ACTIVE),
                    parseEnum(NoticeChannelType.class, fieldText(resource, "channelType", true), null),
                    requiredText(fieldValue(resource, "templateName", true),
                            "MESSAGE_TEMPLATE templateName is required").trim(),
                    requiredText(fieldValue(resource, "titleTemplate", true),
                            "MESSAGE_TEMPLATE titleTemplate is required"),
                    requiredText(fieldValue(resource, "contentTemplate", true),
                            "MESSAGE_TEMPLATE contentTemplate is required"),
                    fieldText(resource, "externalTemplateId", false),
                    fieldText(resource, "variableMapping", false),
                    fieldLong(resource, "channelConfigId", false, null),
                    fieldLong(resource, "operatorId", false, 1L)
            );
        }
    }

    static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("MESSAGE_TEMPLATE field is required: " + name);
        }
        return value;
    }

    static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    static Integer fieldInt(ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    static Boolean fieldBoolean(ResourceDeclaration resource, String name, boolean required, Boolean defaultValue) {
        return toBoolean(fieldValue(resource, name, required), required, defaultValue);
    }

    static String requiredText(Object value, String message) {
        String text = toText(value);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException(message);
        }
        return text;
    }

    static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("MESSAGE_TEMPLATE long value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    static Integer toInt(Object value, boolean required, Integer defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("MESSAGE_TEMPLATE int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    static Boolean toBoolean(Object value, boolean required, Boolean defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("MESSAGE_TEMPLATE boolean value is required");
            }
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, E defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Enum.valueOf(enumType, value.trim().toUpperCase());
    }
}
