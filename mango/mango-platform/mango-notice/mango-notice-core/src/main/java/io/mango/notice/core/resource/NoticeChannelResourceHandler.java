package io.mango.notice.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelCapabilityMode;
import io.mango.notice.api.enums.NoticeChannelSecretStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeChannelConfigRouteTagEntity;
import io.mango.notice.core.entity.NoticeChannelRouteTagEntity;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigRouteTagMapper;
import io.mango.notice.core.mapper.NoticeChannelRouteTagMapper;
import io.mango.notice.core.service.NoticeChannelCapabilityPolicy;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** 通知渠道资源处理器。 */
@Component
@RequiredArgsConstructor
public class NoticeChannelResourceHandler implements ResourceHandler {
    private static final String TARGET_TABLE = "notice_channel_config";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final int DEFAULT_ROUTE_WEIGHT = 100;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS =
            Set.of(
                    "secret",
                    "password",
                    "token",
                    "appsecret",
                    "accesskeysecret",
                    "accesssecret",
                    "secretkey",
                    "smtppassword",
                    "corpsecret",
                    "callbacktoken",
                    "encodingaeskey",
                    "callbackencodingaeskey");

    private final NoticeChannelConfigMapper channelConfigMapper;
    private final NoticeChannelRouteTagMapper routeTagMapper;
    private final NoticeChannelConfigRouteTagMapper configRouteTagMapper;

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
                .requiredField("configCode")
                .fieldDescription("channelConfigId", "通知渠道配置稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("configCode", "通知渠道配置稳定编码；同租户唯一，发布后不可变。")
                .fieldDescription(
                        "channelType", "渠道类型：SITE、SMS、EMAIL、WECHAT_OFFICIAL、WECOM、DINGTALK。")
                .fieldDescription("capabilityMode", "渠道用途：SEND、RECEIVE 或 BOTH；默认 SEND。")
                .fieldDescription("providerCode", "渠道服务商编码，同一租户同一渠道内唯一。")
                .fieldDescription("configName", "渠道配置名称。")
                .fieldDescription("configJson", "非敏感渠道配置 JSON；出现明文 Secret 将拒绝同步。")
                .fieldDescription("secretRefs", "Secret 引用 JSON；值仅支持 env:NAME 或 property:path。")
                .fieldDescription("routeTagCodes", "绑定的路由标签编码 JSON 数组；标签须已存在。")
                .fieldDescription("rateLimitConfig", "限流配置 JSON。")
                .fieldDescription("tenantId", "租户 ID，默认 1。")
                .fieldDescription("enabled", "是否启用，默认 true。")
                .fieldDescription("priority", "优先级，默认 0。")
                .fieldDescription("weight", "权重，默认 100。")
                .fieldDescription("lastSendStatus", "最近发送状态，默认 NONE。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        ChannelPayload payload = ChannelPayload.from(resource);
        return withTenant(payload.tenantId(), () -> upsertInTenant(payload));
    }

    private ResourceSyncResult upsertInTenant(ChannelPayload payload) {
        if (!NoticeChannelCapabilityPolicy.supportsMode(
                payload.channelType(), payload.capabilityMode())) {
            throw new IllegalStateException("MESSAGE_CHANNEL capabilityMode only supports SEND for this channel");
        }
        NoticeChannelConfigEntity entity = find(payload.tenantId(), payload.configCode());
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
        replaceRouteTags(entity, payload.routeTagCodes());
        return ResourceSyncResult.of(
                entity.getId(),
                TARGET_TABLE,
                "Notice channel synced: " + payload.tenantId() + ":" + payload.configCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID);
        return withTenant(tenantId, () -> disableInTenant(resource));
    }

    private ResourceSyncResult disableInTenant(ResourceDeclaration resource) {
        NoticeChannelConfigEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Notice channel not found");
        }
        entity.setEnabled(false);
        entity.setUpdatedAt(LocalDateTime.now());
        channelConfigMapper.updateById(entity);
        return ResourceSyncResult.of(
                entity.getId(),
                TARGET_TABLE,
                "Notice channel disabled: "
                        + entity.getTenantId()
                        + ":"
                        + entity.getChannelType()
                        + ":"
                        + entity.getProviderCode());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        String tenantId = defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID);
        return withTenant(tenantId, () -> deleteInTenant(resource));
    }

    private ResourceSyncResult deleteInTenant(ResourceDeclaration resource) {
        NoticeChannelConfigEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Notice channel not found");
        }
        configRouteTagMapper.delete(
                new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
                        .eq(NoticeChannelConfigRouteTagEntity::getChannelConfigId, entity.getId()));
        channelConfigMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(
                entity.getId(),
                TARGET_TABLE,
                "Notice channel deleted: "
                        + entity.getTenantId()
                        + ":"
                        + entity.getChannelType()
                        + ":"
                        + entity.getProviderCode());
    }

    private void apply(NoticeChannelConfigEntity entity, ChannelPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setChannelType(payload.channelType());
        entity.setCapabilityMode(payload.capabilityMode());
        entity.setConfigCode(payload.configCode());
        entity.setProviderCode(payload.providerCode());
        entity.setConfigName(payload.configName());
        entity.setConfigJson(payload.configJson());
        entity.setSecretRefsJson(toJson(payload.secretRefs()));
        entity.setResourceId(payload.resourceId());
        entity.setResourceVersion(payload.resourceVersion());
        entity.setResourceModuleCode(payload.resourceModuleCode());
        entity.setResourceSource("RESOURCE");
        entity.setManagedFieldsJson(
                toJson(
                        List.of(
                                "channelType",
                                "capabilityMode",
                                "providerCode",
                                "configName",
                                "configJson",
                                "secretRefs",
                                "enabled",
                                "priority",
                                "weight",
                                "rateLimitConfig",
                                "routeTagCodes")));
        Map<String, String> manualSecrets = stringMap(entity.getSecretConfigJson());
        entity.setSecretStatus(
                resolveSecretStatus(
                        payload.channelType(),
                        payload.providerCode(),
                        payload.capabilityMode(),
                        payload.configJson(),
                        payload.secretRefs(),
                        manualSecrets));
        entity.setEnabled(payload.enabled());
        entity.setPriority(payload.priority());
        entity.setWeight(payload.weight());
        entity.setConfigStatus(
                resolveConfigStatus(
                        payload.channelType(),
                        payload.providerCode(),
                        payload.capabilityMode(),
                        payload.configJson(),
                        payload.secretRefs(),
                        manualSecrets));
        entity.setLastSendStatus(payload.lastSendStatus());
        entity.setRateLimitConfig(payload.rateLimitConfig());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private NoticeChannelConfigEntity resolve(ResourceDeclaration resource) {
        String tenantId = defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID);
        String configCode = fieldText(resource, "configCode", false);
        if (StringUtils.hasText(configCode)) {
            NoticeChannelConfigEntity entity = find(tenantId, configCode.trim());
            if (entity != null) {
                return entity;
            }
        }
        String resourceId = resource.getId();
        if (StringUtils.hasText(resourceId)) {
            NoticeChannelConfigEntity entity =
                    channelConfigMapper.selectOne(
                            new LambdaQueryWrapper<NoticeChannelConfigEntity>()
                                    .eq(NoticeChannelConfigEntity::getTenantId, tenantId)
                                    .eq(NoticeChannelConfigEntity::getResourceId, resourceId)
                                    .last("limit 1"));
            if (entity != null) {
                return entity;
            }
        }
        NoticeChannelType channelType =
                parseEnum(NoticeChannelType.class, fieldText(resource, "channelType", false), null);
        String providerCode = fieldText(resource, "providerCode", false);
        if (channelType != null && StringUtils.hasText(providerCode)) {
            NoticeChannelConfigEntity entity =
                    findLegacy(tenantId, channelType, providerCode.trim());
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

    private NoticeChannelConfigEntity find(String tenantId, String configCode) {
        return channelConfigMapper.selectOne(
                new LambdaQueryWrapper<NoticeChannelConfigEntity>()
                        .eq(NoticeChannelConfigEntity::getTenantId, tenantId)
                        .eq(NoticeChannelConfigEntity::getConfigCode, configCode)
                        .last("limit 1"));
    }

    private NoticeChannelConfigEntity findLegacy(
            String tenantId, NoticeChannelType channelType, String providerCode) {
        return channelConfigMapper.selectOne(
                new LambdaQueryWrapper<NoticeChannelConfigEntity>()
                        .eq(NoticeChannelConfigEntity::getTenantId, tenantId)
                        .eq(NoticeChannelConfigEntity::getChannelType, channelType)
                        .eq(NoticeChannelConfigEntity::getProviderCode, providerCode)
                        .last("limit 1"));
    }

    private <T> T withTenant(String tenantId, Supplier<T> action) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(tenantId));
            return action.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private void replaceRouteTags(NoticeChannelConfigEntity config, List<String> tagCodes) {
        configRouteTagMapper.delete(
                new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
                        .eq(NoticeChannelConfigRouteTagEntity::getChannelConfigId, config.getId()));
        for (String tagCode : tagCodes) {
            NoticeChannelRouteTagEntity tag =
                    routeTagMapper.selectOne(
                            new LambdaQueryWrapper<NoticeChannelRouteTagEntity>()
                                    .eq(
                                            NoticeChannelRouteTagEntity::getChannelType,
                                            config.getChannelType())
                                    .eq(NoticeChannelRouteTagEntity::getTagCode, tagCode)
                                    .last("limit 1"));
            if (tag == null) {
                throw new IllegalStateException("MESSAGE_CHANNEL route tag not found: " + tagCode);
            }
            NoticeChannelConfigRouteTagEntity relation = new NoticeChannelConfigRouteTagEntity();
            relation.setChannelConfigId(config.getId());
            relation.setRouteTagId(tag.getId());
            configRouteTagMapper.insert(relation);
        }
    }

    private static String validatedConfigJson(Object value) {
        if (value == null) {
            return null;
        }
        Map<String, Object> config =
                objectMap(value, "MESSAGE_CHANNEL configJson must be a JSON object");
        if (containsPlaintextSecret(config)) {
            throw new IllegalStateException(
                    "MESSAGE_CHANNEL configJson must not contain plaintext Secret values");
        }
        return toJson(config);
    }

    private static boolean containsPlaintextSecret(Map<String, Object> config) {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (SENSITIVE_KEYS.contains(key) || key.endsWith("password") || key.endsWith("token")) {
                return entry.getValue() != null
                        && StringUtils.hasText(String.valueOf(entry.getValue()));
            }
            if (entry.getValue() instanceof Map<?, ?> nested
                    && containsPlaintextSecret(castMap(nested))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> secretRefs(Object value) {
        Map<String, Object> raw =
                value == null
                        ? Collections.emptyMap()
                        : objectMap(value, "MESSAGE_CHANNEL secretRefs must be a JSON object");
        Map<String, String> refs = new LinkedHashMap<>();
        raw.forEach(
                (key, reference) -> {
                    String text = reference == null ? null : String.valueOf(reference).trim();
                    if (!StringUtils.hasText(key)
                            || !StringUtils.hasText(text)
                            || !(text.startsWith("env:") || text.startsWith("property:"))) {
                        throw new IllegalStateException(
                                "MESSAGE_CHANNEL Secret reference must use env: or property:");
                    }
                    refs.put(key, text);
                });
        return Map.copyOf(refs);
    }

    private static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        Object parsed = parseJsonValue(value);
        if (!(parsed instanceof List<?> values)) {
            throw new IllegalStateException("MESSAGE_CHANNEL routeTagCodes must be a JSON array");
        }
        List<String> result = new ArrayList<>();
        for (Object item : values) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item).trim());
            }
        }
        return result.stream().distinct().toList();
    }

    private static NoticeChannelSecretStatus resolveSecretStatus(
            NoticeChannelType channelType,
            String providerCode,
            NoticeChannelCapabilityMode capabilityMode,
            String configJson,
            Map<String, String> refs,
            Map<String, String> manualSecrets) {
        if (channelType == NoticeChannelType.SITE) {
            return NoticeChannelSecretStatus.NOT_REQUIRED;
        }
        Map<String, Object> effective =
                new LinkedHashMap<>(objectMap(configJson, "Invalid channel config"));
        refs.keySet().forEach(key -> effective.put(key, "referenced"));
        manualSecrets.forEach(effective::putIfAbsent);
        NoticeChannelCapabilityMode mode = NoticeChannelCapabilityPolicy.normalize(capabilityMode);
        boolean complete = (!mode.supportsSend()
                        || isSendSecretComplete(channelType, providerCode, effective))
                && (!mode.supportsReceive()
                        || NoticeChannelCapabilityPolicy.missingSecretKeys(
                                        channelType,
                                        providerCode,
                                        NoticeChannelCapabilityMode.RECEIVE,
                                        effective)
                                .isEmpty());
        return complete ? NoticeChannelSecretStatus.COMPLETE : NoticeChannelSecretStatus.INCOMPLETE;
    }

    private static NoticeChannelConfigStatus resolveConfigStatus(
            NoticeChannelType channelType,
            String providerCode,
            NoticeChannelCapabilityMode capabilityMode,
            String configJson,
            Map<String, String> refs,
            Map<String, String> manualSecrets) {
        if (channelType == NoticeChannelType.SITE) {
            return NoticeChannelConfigStatus.COMPLETE;
        }
        Map<String, Object> config =
                new LinkedHashMap<>(objectMap(configJson, "Invalid channel config"));
        refs.keySet().forEach(key -> config.put(key, "referenced"));
        manualSecrets.forEach(config::putIfAbsent);
        if (config.isEmpty()) {
            return NoticeChannelConfigStatus.INCOMPLETE;
        }
        NoticeChannelCapabilityMode mode = NoticeChannelCapabilityPolicy.normalize(capabilityMode);
        boolean complete = (!mode.supportsSend()
                        || isSendConfigComplete(channelType, providerCode, config))
                && (!mode.supportsReceive()
                        || NoticeChannelCapabilityPolicy.isConfigComplete(
                                channelType,
                                providerCode,
                                NoticeChannelCapabilityMode.RECEIVE,
                                config));
        return complete ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
    }

    private static boolean isSendSecretComplete(
            NoticeChannelType channelType, String providerCode, Map<String, Object> config) {
        return switch (channelType) {
            case EMAIL ->
                    "ALIYUN_DM".equalsIgnoreCase(providerCode)
                            ? hasAny(config, "accessKeySecret", "accessSecret")
                            : hasAny(config, "password", "smtpPassword");
            case SMS ->
                    "TENCENT_SMS".equalsIgnoreCase(providerCode)
                            ? hasAny(config, "secretKey")
                            : hasAny(config, "accessKeySecret", "accessSecret");
            case WECHAT_OFFICIAL -> hasAny(config, "appSecret", "secret");
            case WECOM -> hasAny(config, "secret", "corpSecret", "webhookUrl");
            case DINGTALK -> hasAny(config, "appSecret", "secret", "webhookUrl");
            case SITE -> true;
        };
    }

    private static boolean isSendConfigComplete(
            NoticeChannelType channelType, String providerCode, Map<String, Object> config) {
        return switch (channelType) {
            case EMAIL -> isSendEmailConfigComplete(providerCode, config);
            case SMS ->
                    hasAny(config, "accessKeyId", "accessKey", "secretId")
                            && hasAny(config, "accessKeySecret", "accessSecret", "secretKey")
                            && hasAny(config, "signName", "sign");
            case WECHAT_OFFICIAL ->
                    hasAny(config, "appId") && hasAny(config, "appSecret", "secret");
            case WECOM ->
                    hasAny(config, "corpId")
                            && hasAny(config, "agentId", "webhookUrl")
                            && hasAny(config, "secret", "corpSecret", "webhookUrl");
            case DINGTALK ->
                    hasAny(config, "appKey", "webhookUrl")
                            && hasAny(config, "appSecret", "webhookUrl");
            case SITE -> true;
        };
    }

    private static boolean isSendEmailConfigComplete(
            String providerCode, Map<String, Object> config) {
        if ("ALIYUN_DM".equalsIgnoreCase(providerCode)) {
            return hasAny(config, "accessKeyId", "accessKey")
                    && hasAny(config, "accessKeySecret", "accessSecret")
                    && hasAny(config, "accountName", "from", "fromAddress");
        }
        return hasAny(config, "host", "smtpHost")
                && hasAny(config, "username", "account")
                && hasAny(config, "password", "smtpPassword")
                && hasAny(config, "from", "fromAddress");
    }

    private static boolean hasAny(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object value = config.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> stringMap(String json) {
        Map<String, Object> values = objectMap(json, "Invalid Secret config");
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach(
                (key, value) -> {
                    if (value != null && StringUtils.hasText(String.valueOf(value))) {
                        result.put(key, String.valueOf(value));
                    }
                });
        return result;
    }

    private static Map<String, Object> objectMap(Object value, String message) {
        if (value == null || (value instanceof String text && !StringUtils.hasText(text))) {
            return Collections.emptyMap();
        }
        Object parsed = parseJsonValue(value);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalStateException(message);
        }
        return castMap(map);
    }

    private static Object parseJsonValue(Object value) {
        if (!(value instanceof String text)) {
            return value;
        }
        try {
            return OBJECT_MAPPER.readValue(text, Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("MESSAGE_CHANNEL field contains invalid JSON", ex);
        }
    }

    private static Map<String, Object> castMap(Map<?, ?> map) {
        return OBJECT_MAPPER.convertValue(map, new TypeReference<>() {});
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("MESSAGE_CHANNEL field cannot be serialized", ex);
        }
    }

    private record ChannelPayload(
            Long channelConfigId,
            String resourceId,
            Integer resourceVersion,
            String resourceModuleCode,
            String tenantId,
            String configCode,
            NoticeChannelType channelType,
            NoticeChannelCapabilityMode capabilityMode,
            String providerCode,
            String configName,
            String configJson,
            Boolean enabled,
            Integer priority,
            Integer weight,
            NoticeChannelSendHealthStatus lastSendStatus,
            String rateLimitConfig,
            Map<String, String> secretRefs,
            List<String> routeTagCodes) {
        private static ChannelPayload from(ResourceDeclaration resource) {
            return new ChannelPayload(
                    fieldLong(resource, "channelConfigId", false, Long.valueOf(resource.getId())),
                    resource.getId(),
                    resource.getVersion(),
                    resource.getModuleCode(),
                    defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID),
                    requiredText(
                                    fieldValue(resource, "configCode", true),
                                    "MESSAGE_CHANNEL configCode is required")
                            .trim(),
                    parseEnum(
                            NoticeChannelType.class,
                            fieldText(resource, "channelType", true),
                            null),
                    parseEnum(
                            NoticeChannelCapabilityMode.class,
                            fieldText(resource, "capabilityMode", false),
                            NoticeChannelCapabilityMode.SEND),
                    requiredText(
                                    fieldValue(resource, "providerCode", true),
                                    "MESSAGE_CHANNEL providerCode is required")
                            .trim(),
                    requiredText(
                                    fieldValue(resource, "configName", true),
                                    "MESSAGE_CHANNEL configName is required")
                            .trim(),
                    validatedConfigJson(fieldValue(resource, "configJson", false)),
                    fieldBoolean(resource, "enabled", false, true),
                    fieldInt(resource, "priority", false, 0),
                    fieldInt(resource, "weight", false, DEFAULT_ROUTE_WEIGHT),
                    parseEnum(
                            NoticeChannelSendHealthStatus.class,
                            fieldText(resource, "lastSendStatus", false),
                            NoticeChannelSendHealthStatus.NONE),
                    fieldText(resource, "rateLimitConfig", false),
                    NoticeChannelResourceHandler.secretRefs(
                            fieldValue(resource, "secretRefs", false)),
                    stringList(fieldValue(resource, "routeTagCodes", false)));
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

    static Long fieldLong(
            ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    static Integer fieldInt(
            ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    static Boolean fieldBoolean(
            ResourceDeclaration resource, String name, boolean required, Boolean defaultValue) {
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
