package io.mango.area.core.resource;

import io.mango.area.core.entity.SysAreaEntity;
import io.mango.area.core.mapper.SysAreaMapper;
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

@Component
@RequiredArgsConstructor
public class SystemAreaResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "sys_area";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final String ENABLED = "1";
    private static final String DISABLED = "0";

    private final SysAreaMapper areaMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.SYSTEM_AREA;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("name")
                .requiredField("areaType")
                .fieldDescription("targetId", "行政区划稳定 ID；不填时使用资源 ID。")
                .fieldDescription("pid", "父级 ID，根节点为 0。")
                .fieldDescription("adcode", "行政区划编码。")
                .fieldDescription("areaStatus", "状态：0-未生效 1-生效。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        AreaPayload payload = AreaPayload.from(resource);
        SysAreaEntity entity = areaMapper.selectById(payload.targetId());
        if (entity == null) {
            entity = new SysAreaEntity();
            entity.setId(payload.targetId());
            apply(entity, payload);
            areaMapper.insert(entity);
        } else {
            apply(entity, payload);
            areaMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "System area synced: " + payload.name());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        SysAreaEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System area not found");
        }
        entity.setAreaStatus(DISABLED);
        areaMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "System area disabled: " + entity.getName());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        return disable(resource);
    }

    private SysAreaEntity resolve(ResourceDeclaration resource) {
        Long targetId = number(resource, "targetId", false, null);
        if (targetId == null) {
            targetId = Long.valueOf(resource.getId());
        }
        return areaMapper.selectById(targetId);
    }

    private void apply(SysAreaEntity entity, AreaPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setPid(payload.pid());
        entity.setName(payload.name());
        entity.setLetter(payload.letter());
        entity.setAdcode(payload.adcode());
        entity.setLocation(payload.location());
        entity.setAreaSort(payload.areaSort());
        entity.setAreaStatus(payload.areaStatus());
        entity.setAreaType(payload.areaType());
        entity.setHot(payload.hot());
        entity.setCityCode(payload.cityCode());
        if (!StringUtils.hasText(entity.getTenantId())) {
            entity.setTenantId(DEFAULT_TENANT_ID);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private record AreaPayload(Long targetId, Long pid, String name, String letter, Long adcode, String location,
                               Integer areaSort, String areaStatus, String areaType, String hot, String cityCode) {

        private static AreaPayload from(ResourceDeclaration resource) {
            return new AreaPayload(
                    number(resource, "targetId", false, Long.valueOf(resource.getId())),
                    number(resource, "pid", false, 0L),
                    requiredText(resource, "name"),
                    text(resource, "letter", false),
                    number(resource, "adcode", false, null),
                    text(resource, "location", false),
                    integer(resource, "areaSort", 0),
                    defaultText(text(resource, "areaStatus", false), ENABLED),
                    requiredText(resource, "areaType"),
                    defaultText(text(resource, "hot", false), DISABLED),
                    text(resource, "cityCode", false));
        }
    }

    private static String requiredText(ResourceDeclaration resource, String name) {
        String value = text(resource, name, true);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("SYSTEM_AREA field is required: " + name);
        }
        return value.trim();
    }

    private static String text(ResourceDeclaration resource, String name, boolean required) {
        Object value = value(resource, name, required);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static String defaultText(String value, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value;
        }
        return defaultValue;
    }

    private static Long number(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        Object value = value(resource, name, required);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer integer(ResourceDeclaration resource, String name, Integer defaultValue) {
        Object value = value(resource, name, false);
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static Object value(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = null;
        if (field != null) {
            value = field.getValue();
        }
        if (required && value == null) {
            throw new IllegalStateException("SYSTEM_AREA field is required: " + name);
        }
        return value;
    }
}
