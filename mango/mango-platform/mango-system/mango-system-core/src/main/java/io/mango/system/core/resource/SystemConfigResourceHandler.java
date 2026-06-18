package io.mango.system.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.system.api.enums.ConfigTypeEnum;
import io.mango.system.core.entity.SysConfig;
import io.mango.system.core.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统参数资源处理器。
 */
@Component
@RequiredArgsConstructor
public class SystemConfigResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "sys_config";
    private static final String DEFAULT_DOMAIN_CODE = "COMMON";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final SysConfigMapper sysConfigMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.SYSTEM_CONFIG;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("configKey")
                .requiredField("configName")
                .requiredField("configValue")
                .fieldDescription("configId", "系统参数稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("configKey", "系统参数 Key，全局唯一。")
                .fieldDescription("configValue", "系统参数值。")
                .fieldDescription("configName", "系统参数名称。")
                .fieldDescription("type", "系统参数类型：SYSTEM、BUSINESS、SECURITY、FEATURE。")
                .fieldDescription("domainCode", "业务域编码，默认 COMMON。")
                .fieldDescription("sort", "排序号，默认 0。")
                .fieldDescription("status", "状态：1 启用，0 禁用。")
                .fieldDescription("remark", "备注。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        ConfigPayload payload = ConfigPayload.from(resource);
        SysConfig config = findByConfigKey(payload.configKey());
        if (config == null) {
            config = new SysConfig();
            config.setId(payload.configId());
            config.setConfigKey(payload.configKey());
            applyConfig(config, payload);
            sysConfigMapper.insert(config);
        } else {
            applyConfig(config, payload);
            sysConfigMapper.updateById(config);
        }
        return ResourceSyncResult.of(config.getId(), TARGET_TABLE, "System config synced: " + payload.configKey());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        SysConfig config = resolveConfig(resource);
        if (config == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System config not found");
        }
        config.setStatus(DISABLED);
        config.setUpdateTime(LocalDateTime.now());
        sysConfigMapper.updateById(config);
        return ResourceSyncResult.of(config.getId(), TARGET_TABLE, "System config disabled: " + config.getConfigKey());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        SysConfig config = resolveConfig(resource);
        if (config == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System config not found");
        }
        sysConfigMapper.deleteById(config.getId());
        return ResourceSyncResult.of(config.getId(), TARGET_TABLE, "System config deleted: " + config.getConfigKey());
    }

    private void applyConfig(SysConfig config, ConfigPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        config.setConfigKey(payload.configKey());
        config.setConfigValue(payload.configValue());
        config.setConfigName(payload.configName());
        config.setType(payload.type());
        config.setDomainCode(payload.domainCode());
        config.setSort(payload.sort());
        config.setStatus(payload.status());
        config.setRemark(payload.remark());
        if (config.getCreateTime() == null) {
            config.setCreateTime(now);
        }
        config.setUpdateTime(now);
    }

    private SysConfig resolveConfig(ResourceDeclaration resource) {
        String configKey = fieldText(resource, "configKey", false);
        if (StringUtils.hasText(configKey)) {
            SysConfig config = findByConfigKey(configKey.trim());
            if (config != null) {
                return config;
            }
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId != null) {
            return sysConfigMapper.selectById(targetId);
        }
        Long configId = fieldLong(resource, "configId", false, null);
        return configId == null ? null : sysConfigMapper.selectById(configId);
    }

    private SysConfig findByConfigKey(String configKey) {
        return sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .last("limit 1"));
    }

    private record ConfigPayload(Long configId, String configKey, String configValue, String configName,
                                 ConfigTypeEnum type, String domainCode, Integer sort, Integer status,
                                 String remark) {

        private static ConfigPayload from(ResourceDeclaration resource) {
            return new ConfigPayload(
                    fieldLong(resource, "configId", false, Long.valueOf(resource.getId())),
                    requiredText(fieldValue(resource, "configKey", true), "SYSTEM_CONFIG configKey is required").trim(),
                    requiredText(fieldValue(resource, "configValue", true), "SYSTEM_CONFIG configValue is required"),
                    requiredText(fieldValue(resource, "configName", true), "SYSTEM_CONFIG configName is required").trim(),
                    parseType(fieldText(resource, "type", false)),
                    defaultText(fieldText(resource, "domainCode", false), DEFAULT_DOMAIN_CODE).toUpperCase(),
                    fieldInt(resource, "sort", false, 0),
                    normalizeStatus(fieldInt(resource, "status", false, ENABLED)),
                    fieldText(resource, "remark", false)
            );
        }
    }

    private static ConfigTypeEnum parseType(String value) {
        if (!StringUtils.hasText(value)) {
            return ConfigTypeEnum.SYSTEM;
        }
        return ConfigTypeEnum.valueOf(value.trim().toUpperCase());
    }

    private static Integer normalizeStatus(Integer status) {
        if (status == null) {
            return ENABLED;
        }
        if (status != ENABLED && status != DISABLED) {
            throw new IllegalStateException("SYSTEM_CONFIG status is invalid: " + status);
        }
        return status;
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Integer fieldInt(ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        return toInt(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("SYSTEM_CONFIG field is required: " + name);
        }
        return value;
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
                throw new IllegalStateException("SYSTEM_CONFIG long value is required");
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
                throw new IllegalStateException("SYSTEM_CONFIG int value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
