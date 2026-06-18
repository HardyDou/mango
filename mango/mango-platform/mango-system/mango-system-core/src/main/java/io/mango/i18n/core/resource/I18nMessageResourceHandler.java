package io.mango.i18n.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.i18n.api.entity.SysI18n;
import io.mango.i18n.core.mapper.SysI18nMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 国际化文案资源处理器。
 */
@Component
@RequiredArgsConstructor
public class I18nMessageResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "sys_i18n";

    private final SysI18nMapper sysI18nMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.I18N_MESSAGE;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("name")
                .requiredField("zhCn")
                .requiredField("en")
                .fieldDescription("i18nId", "国际化条目稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("tenantId", "租户 ID，当前 sys_i18n 表默认 1。")
                .fieldDescription("name", "国际化 Key，全局唯一。")
                .fieldDescription("zhCn", "中文文案。")
                .fieldDescription("en", "英文文案。")
                .fieldDescription("description", "文案说明。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        I18nPayload payload = I18nPayload.from(resource);
        SysI18n entity = findByName(payload.name());
        if (entity == null) {
            entity = new SysI18n();
            entity.setId(payload.i18nId());
            entity.setName(payload.name());
            applyI18n(entity, payload);
            sysI18nMapper.insert(entity);
        } else {
            applyI18n(entity, payload);
            sysI18nMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "I18n message synced: " + payload.name());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        return delete(resource);
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        SysI18n entity = resolveI18n(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "I18n message not found");
        }
        sysI18nMapper.deleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "I18n message deleted: " + entity.getName());
    }

    private void applyI18n(SysI18n entity, I18nPayload payload) {
        entity.setName(payload.name());
        entity.setZhCn(payload.zhCn());
        entity.setEn(payload.en());
        entity.setDescription(payload.description());
    }

    private SysI18n resolveI18n(ResourceDeclaration resource) {
        String name = fieldText(resource, "name", false);
        if (StringUtils.hasText(name)) {
            SysI18n entity = findByName(name.trim());
            if (entity != null) {
                return entity;
            }
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        if (targetId != null) {
            return sysI18nMapper.selectById(targetId);
        }
        Long i18nId = fieldLong(resource, "i18nId", false, null);
        return i18nId == null ? null : sysI18nMapper.selectById(i18nId);
    }

    private SysI18n findByName(String name) {
        return sysI18nMapper.selectOne(new LambdaQueryWrapper<SysI18n>()
                .eq(SysI18n::getName, name)
                .last("limit 1"));
    }

    private record I18nPayload(Long i18nId, String name, String zhCn, String en, String description) {

        private static I18nPayload from(ResourceDeclaration resource) {
            return new I18nPayload(
                    fieldLong(resource, "i18nId", false, Long.valueOf(resource.getId())),
                    requiredText(fieldValue(resource, "name", true), "I18N_MESSAGE name is required").trim(),
                    requiredText(fieldValue(resource, "zhCn", true), "I18N_MESSAGE zhCn is required"),
                    requiredText(fieldValue(resource, "en", true), "I18N_MESSAGE en is required"),
                    fieldText(resource, "description", false)
            );
        }
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        return toText(fieldValue(resource, name, required));
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        return toLong(fieldValue(resource, name, required), required, defaultValue);
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("I18N_MESSAGE field is required: " + name);
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

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value, boolean required, Long defaultValue) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            if (required) {
                throw new IllegalStateException("I18N_MESSAGE long value is required");
            }
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
