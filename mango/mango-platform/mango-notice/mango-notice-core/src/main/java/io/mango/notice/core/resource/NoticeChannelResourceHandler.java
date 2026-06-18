package io.mango.notice.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
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
 * 通知渠道资源处理器。
 */
@Component
@RequiredArgsConstructor
public class NoticeChannelResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "notice_channel_config";
    private static final String DEFAULT_TENANT_ID = "1";

    private final NoticeChannelConfigMapper channelConfigMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.MESSAGE_CHANNEL;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("channelType")
                .requiredField("providerCode")
                .requiredField("configName")
                .fieldDescription("channelConfigId", "通知渠道配置稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("channelType", "渠道类型：SITE、SMS、EMAIL、WECHAT_OFFICIAL、WECOM、DINGTALK。")
                .fieldDescription("providerCode", "渠道服务商编码，同一租户同一渠道内唯一。")
                .fieldDescription("configName", "渠道配置名称。")
                .fieldDescription("configJson", "渠道配置 JSON。")
                .fieldDescription("rateLimitConfig", "限流配置 JSON。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("enabled", "是否启用，默认 true。")
                .fieldDescription("priority", "优先级，默认 0。")
                .fieldDescription("weight", "权重，默认 100。")
                .fieldDescription("configStatus", "配置状态，默认 COMPLETE。")
                .fieldDescription("lastSendStatus", "最近发送状态，默认 NONE。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        ChannelPayload payload = ChannelPayload.from(resource);
        NoticeChannelConfigEntity entity = find(payload.tenantId(), payload.channelType(), payload.providerCode());
        if (entity == null) {
            entity = new NoticeChannelConfigEntity();
            entity.setId(payload.channelConfigId());
            entity.setTenantId(payload.tenantId());
            entity.setChannelType(payload.channelType());
            entity.setProviderCode(payload.providerCode());
            apply(entity, payload);
            channelConfigMapper.insert(entity);
        } else {
            apply(entity, payload);
            channelConfigMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Notice channel synced: " + payload.tenantId() + ":" + payload.channelType() + ":" + payload.providerCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        NoticeChannelConfigEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Notice channel not found");
        }
        entity.setEnabled(false);
        entity.setUpdatedAt(LocalDateTime.now());
        channelConfigMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Notice channel disabled: " + entity.getTenantId() + ":" + entity.getChannelType() + ":" + entity.getProviderCode());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        NoticeChannelConfigEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Notice channel not found");
        }
        channelConfigMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Notice channel deleted: " + entity.getTenantId() + ":" + entity.getChannelType() + ":" + entity.getProviderCode());
    }

    private void apply(NoticeChannelConfigEntity entity, ChannelPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setChannelType(payload.channelType());
        entity.setProviderCode(payload.providerCode());
        entity.setConfigName(payload.configName());
        entity.setConfigJson(payload.configJson());
        entity.setEnabled(payload.enabled());
        entity.setPriority(payload.priority());
        entity.setWeight(payload.weight());
        entity.setConfigStatus(payload.configStatus());
        entity.setLastSendStatus(payload.lastSendStatus());
        entity.setRateLimitConfig(payload.rateLimitConfig());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private NoticeChannelConfigEntity resolve(ResourceDeclaration resource) {
        String tenantId = defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID);
        NoticeChannelType channelType = parseEnum(NoticeChannelType.class, fieldText(resource, "channelType", false), null);
        String providerCode = fieldText(resource, "providerCode", false);
        if (channelType != null && StringUtils.hasText(providerCode)) {
            NoticeChannelConfigEntity entity = find(tenantId, channelType, providerCode.trim());
            if (entity != null) {
                return entity;
            }
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId != null) {
            return channelConfigMapper.selectById(targetId);
        }
        Long channelConfigId = fieldLong(resource, "channelConfigId", false, null);
        return channelConfigId == null ? null : channelConfigMapper.selectById(channelConfigId);
    }

    private NoticeChannelConfigEntity find(String tenantId, NoticeChannelType channelType, String providerCode) {
        return channelConfigMapper.selectOne(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
                .eq(NoticeChannelConfigEntity::getTenantId, tenantId)
                .eq(NoticeChannelConfigEntity::getChannelType, channelType)
                .eq(NoticeChannelConfigEntity::getProviderCode, providerCode)
                .last("limit 1"));
    }

    private record ChannelPayload(Long channelConfigId, String tenantId, NoticeChannelType channelType,
                                  String providerCode, String configName, String configJson, Boolean enabled,
                                  Integer priority, Integer weight, NoticeChannelConfigStatus configStatus,
                                  NoticeChannelSendHealthStatus lastSendStatus, String rateLimitConfig) {

        private static ChannelPayload from(ResourceDeclaration resource) {
            return new ChannelPayload(
                    fieldLong(resource, "channelConfigId", false, Long.valueOf(resource.getId())),
                    defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID),
                    parseEnum(NoticeChannelType.class, fieldText(resource, "channelType", true), null),
                    requiredText(fieldValue(resource, "providerCode", true), "MESSAGE_CHANNEL providerCode is required").trim(),
                    requiredText(fieldValue(resource, "configName", true), "MESSAGE_CHANNEL configName is required").trim(),
                    fieldText(resource, "configJson", false),
                    fieldBoolean(resource, "enabled", false, true),
                    fieldInt(resource, "priority", false, 0),
                    fieldInt(resource, "weight", false, 100),
                    parseEnum(NoticeChannelConfigStatus.class, fieldText(resource, "configStatus", false),
                            NoticeChannelConfigStatus.COMPLETE),
                    parseEnum(NoticeChannelSendHealthStatus.class, fieldText(resource, "lastSendStatus", false),
                            NoticeChannelSendHealthStatus.NONE),
                    fieldText(resource, "rateLimitConfig", false)
            );
        }
    }

    static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("MESSAGE_CHANNEL field is required: " + name);
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
                throw new IllegalStateException("MESSAGE_CHANNEL long value is required");
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
                throw new IllegalStateException("MESSAGE_CHANNEL int value is required");
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
                throw new IllegalStateException("MESSAGE_CHANNEL boolean value is required");
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
