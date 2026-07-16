package io.mango.system.core.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import io.mango.system.core.entity.SysTenantEntity;
import io.mango.system.core.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SystemTenantResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "sys_tenant";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private final SysTenantMapper tenantMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.SYSTEM_TENANT;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantName")
                .requiredField("tenantCode")
                .fieldDescription("targetId", "租户稳定 ID；不填时使用资源 ID。")
                .fieldDescription("tenantName", "租户名称。")
                .fieldDescription("tenantCode", "租户编码，全局唯一。")
                .fieldDescription("institutionType", "机构类型，默认 ENTERPRISE。")
                .fieldDescription("capabilityCodes", "开通能力编码，逗号分隔。")
                .fieldDescription("packageId", "菜单授权套餐 ID。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        TenantPayload payload = TenantPayload.from(resource);
        SysTenantEntity entity = findByCode(payload.tenantCode());
        if (entity == null) {
            entity = new SysTenantEntity();
            entity.setId(payload.targetId());
            entity.setTenantCode(payload.tenantCode());
            apply(entity, payload);
            tenantMapper.insert(entity);
        } else {
            apply(entity, payload);
            tenantMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "System tenant synced: " + payload.tenantCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        SysTenantEntity entity = resolve(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "System tenant not found");
        }
        entity.setStatus(DISABLED);
        tenantMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "System tenant disabled: " + entity.getTenantCode());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        return disable(resource);
    }

    private void apply(SysTenantEntity entity, TenantPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        entity.setTenantName(payload.tenantName());
        entity.setTenantCode(payload.tenantCode());
        entity.setInstitutionType(payload.institutionType());
        entity.setCapabilityCodes(payload.capabilityCodes());
        entity.setPackageId(payload.packageId());
        entity.setStatus(payload.status());
        entity.setContact(payload.contact());
        entity.setMobile(payload.mobile());
        entity.setEmail(payload.email());
        entity.setRemark(payload.remark());
        if (!StringUtils.hasText(entity.getTenantId())) {
            entity.setTenantId(DEFAULT_TENANT_ID);
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    private SysTenantEntity resolve(ResourceDeclaration resource) {
        String tenantCode = text(resource, "tenantCode", false);
        if (StringUtils.hasText(tenantCode)) {
            SysTenantEntity byCode = findByCode(tenantCode.trim());
            if (byCode != null) {
                return byCode;
            }
        }
        Long targetId = number(resource, "targetId", false, null);
        if (targetId == null) {
            return null;
        }
        return tenantMapper.selectById(targetId);
    }

    private SysTenantEntity findByCode(String tenantCode) {
        return tenantMapper.selectOne(new LambdaQueryWrapper<SysTenantEntity>()
                .eq(SysTenantEntity::getTenantCode, tenantCode)
                .last("limit 1"));
    }

    private record TenantPayload(Long targetId, String tenantName, String tenantCode, String institutionType,
                                 String capabilityCodes, Long packageId, Integer status, String contact,
                                 String mobile, String email, String remark) {

        private static TenantPayload from(ResourceDeclaration resource) {
            return new TenantPayload(
                    number(resource, "targetId", false, Long.valueOf(resource.getId())),
                    requiredText(resource, "tenantName"),
                    requiredText(resource, "tenantCode"),
                    defaultText(text(resource, "institutionType", false), "ENTERPRISE"),
                    text(resource, "capabilityCodes", false),
                    number(resource, "packageId", false, null),
                    integer(resource, "status", ENABLED),
                    text(resource, "contact", false),
                    text(resource, "mobile", false),
                    text(resource, "email", false),
                    text(resource, "remark", false));
        }
    }

    private static String requiredText(ResourceDeclaration resource, String name) {
        String value = text(resource, name, true);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("SYSTEM_TENANT field is required: " + name);
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
            throw new IllegalStateException("SYSTEM_TENANT field is required: " + name);
        }
        return value;
    }
}
