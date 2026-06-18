package io.mango.domain.core.resource;

import io.mango.domain.core.entity.DomainEntity;
import io.mango.domain.core.mapper.DomainMapper;
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
import java.util.Locale;

/**
 * 业务域资源处理器。
 */
@Component
@RequiredArgsConstructor
public class DomainResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "biz_domain";
    private static final long ROOT_PARENT_ID = 0L;
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final int NOT_DELETED = 0;
    private static final String DEFAULT_TENANT_ID = "1";

    private final DomainMapper domainMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.BUSINESS_DOMAIN;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("domainCode")
                .requiredField("domainShortCode")
                .requiredField("domainName")
                .fieldDescription("domainId", "业务域表稳定 ID，可选；不填时使用资源 ID。")
                .fieldDescription("tenantId", "租户标识，默认 1。")
                .fieldDescription("orgId", "所属组织 ID，可选。")
                .fieldDescription("domainCode", "业务域编码，租户内唯一。")
                .fieldDescription("domainShortCode", "业务域编码简写，租户内唯一。")
                .fieldDescription("domainName", "业务域名称。")
                .fieldDescription("parentId", "父业务域 ID，默认 0。")
                .fieldDescription("sort", "排序号，默认 0。")
                .fieldDescription("status", "状态：1 启用，0 停用。")
                .fieldDescription("remark", "备注。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        DomainPayload payload = DomainPayload.from(resource);
        DomainEntity entity = findByCode(payload.tenantId(), payload.domainCode());
        if (entity == null) {
            entity = new DomainEntity();
            entity.setId(payload.domainId());
            apply(entity, payload);
            entity.setDeleted(NOT_DELETED);
            entity.setCreateTime(LocalDateTime.now());
            entity.setCreatedAt(LocalDateTime.now());
            domainMapper.insert(entity);
        } else {
            apply(entity, payload);
            entity.setDeleted(NOT_DELETED);
            domainMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "Business domain synced: " + payload.domainCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        DomainEntity entity = resolveDomain(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Business domain not found");
        }
        entity.setStatus(DISABLED);
        entity.setUpdateTime(LocalDateTime.now());
        domainMapper.updateById(entity);
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "Business domain disabled: " + entity.getDomainCode());
    }

    @Override
    public ResourceSyncResult delete(ResourceDeclaration resource) {
        DomainEntity entity = resolveDomain(resource);
        if (entity == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Business domain not found");
        }
        domainMapper.physicalDeleteById(entity.getId());
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE, "Business domain deleted: " + entity.getDomainCode());
    }

    private void apply(DomainEntity entity, DomainPayload payload) {
        entity.setTenantId(payload.tenantId());
        entity.setOrgId(payload.orgId());
        entity.setDomainCode(payload.domainCode());
        entity.setDomainShortCode(payload.domainShortCode());
        entity.setDomainName(payload.domainName());
        entity.setParentId(payload.parentId());
        entity.setSort(payload.sort());
        entity.setStatus(payload.status());
        entity.setRemark(payload.remark());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private DomainEntity resolveDomain(ResourceDeclaration resource) {
        String tenantId = defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID);
        String domainCode = fieldText(resource, "domainCode", false);
        if (StringUtils.hasText(domainCode)) {
            DomainEntity entity = findByCode(tenantId, normalizeCode(domainCode));
            if (entity != null) {
                return entity;
            }
        }
        Long targetId = fieldLong(resource, "targetId", false, null);
        return targetId == null ? null : domainMapper.selectByIdIncludingDeleted(targetId);
    }

    private DomainEntity findByCode(String tenantId, String domainCode) {
        return domainMapper.selectByTenantAndCodeIncludingDeleted(tenantId, domainCode);
    }

    private record DomainPayload(Long domainId, String tenantId, Long orgId, String domainCode,
                                 String domainShortCode, String domainName, Long parentId,
                                 Integer sort, Integer status, String remark) {

        private static DomainPayload from(ResourceDeclaration resource) {
            return new DomainPayload(
                    fieldLong(resource, "domainId", false, Long.valueOf(resource.getId())),
                    defaultText(fieldText(resource, "tenantId", false), DEFAULT_TENANT_ID),
                    fieldLong(resource, "orgId", false, null),
                    normalizeCode(fieldText(resource, "domainCode", true)),
                    normalizeCode(fieldText(resource, "domainShortCode", true)),
                    fieldText(resource, "domainName", true).trim(),
                    fieldLong(resource, "parentId", false, ROOT_PARENT_ID),
                    fieldInt(resource, "sort", false, 0),
                    normalizeStatus(fieldInt(resource, "status", false, ENABLED)),
                    defaultText(fieldText(resource, "remark", false), "")
            );
        }
    }

    private static String fieldText(ResourceDeclaration resource, String name, boolean required) {
        Object value = fieldValue(resource, name, required);
        return value == null ? null : String.valueOf(value);
    }

    private static Long fieldLong(ResourceDeclaration resource, String name, boolean required, Long defaultValue) {
        Object value = fieldValue(resource, name, required);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer fieldInt(ResourceDeclaration resource, String name, boolean required, Integer defaultValue) {
        Object value = fieldValue(resource, name, required);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static Object fieldValue(ResourceDeclaration resource, String name, boolean required) {
        ResourceField field = resource.getFields().get(name);
        Object value = field == null ? null : field.getValue();
        if (required && value == null) {
            throw new IllegalStateException("BUSINESS_DOMAIN field is required: " + name);
        }
        return value;
    }

    private static String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("BUSINESS_DOMAIN code is required");
        }
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static Integer normalizeStatus(Integer status) {
        if (status == null) {
            return ENABLED;
        }
        if (status != DISABLED && status != ENABLED) {
            throw new IllegalStateException("BUSINESS_DOMAIN status is invalid: " + status);
        }
        return status;
    }

    private static String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
