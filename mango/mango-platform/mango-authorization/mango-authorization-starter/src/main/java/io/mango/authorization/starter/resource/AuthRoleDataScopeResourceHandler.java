package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.mango.authorization.api.AuthorizationOrgReferenceProvider;
import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleDataScopeEntity;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resource handler for role data scope declarations.
 */
@Component
public class AuthRoleDataScopeResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_role_data_scope";
    private static final String DEFAULT_APP_CODE = "internal-admin";

    private final RoleMapper roleMapper;
    private final RoleDataScopeMapper roleDataScopeMapper;
    private final ObjectProvider<AuthorizationOrgReferenceProvider> orgReferenceProvider;
    private final ObjectWriter scopeValuesWriter;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.AUTH_ROLE_DATA_SCOPE);

    public AuthRoleDataScopeResourceHandler(RoleMapper roleMapper,
                                            RoleDataScopeMapper roleDataScopeMapper,
                                            ObjectProvider<AuthorizationOrgReferenceProvider> orgReferenceProvider,
                                            ObjectMapper objectMapper) {
        this.roleMapper = roleMapper;
        this.roleDataScopeMapper = roleDataScopeMapper;
        this.orgReferenceProvider = orgReferenceProvider;
        this.scopeValuesWriter = objectMapper.writer();
    }

    @Override
    public String resourceType() {
        return ResourceTypes.AUTH_ROLE_DATA_SCOPE;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(ResourceTypes.AUTH_ROLE, ResourceTypes.ORG_UNIT);
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("appCode")
                .requiredField("roleCode")
                .requiredField("resourceCode")
                .requiredField("scopeMode")
                .fieldDescription("roleCode", "角色编码，handler 会解析为角色 ID。")
                .fieldDescription("resourceCode", "数据权限资源编码。")
                .fieldDescription("scopeMode", "ALL、SELF、SELF_ORG、SELF_ORG_AND_CHILDREN、ORG。")
                .fieldDescription("scopeValues", "范围值，ORG 模式下为组织 ID 字符串列表。")
                .fieldDescription("orgCodes", "ORG 模式下的组织编码列表，handler 会解析为组织 ID。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        return ResourceTenantScope.call(tenantId, () -> upsertInTenant(resource));
    }

    private ResourceSyncResult upsertInTenant(ResourceDeclaration resource) {
        RoleEntity role = requiredRole(resource);
        String resourceCode = fields.requiredString(resource, "resourceCode");
        RoleDataScopeEntity entity = findScope(role, resourceCode);
        if (entity != null && resource.getSyncMode() == ResourceSyncMode.INIT_ONLY) {
            return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                    "Auth role data scope preserved for INIT_ONLY: " + role.getRoleCode() + "/" + resourceCode);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean creating = entity == null;
        if (creating) {
            entity = new RoleDataScopeEntity();
            entity.setId(fields.targetIdOrStable(resource, TARGET_TABLE,
                    role.getTenantId(), role.getAppCode(),
                    fields.requiredString(resource, "roleCode"), resourceCode));
            entity.setTenantId(role.getTenantId());
            entity.setAppCode(role.getAppCode());
            entity.setRoleId(role.getRoleId());
            entity.setResourceCode(resourceCode);
            entity.setCreateTime(now);
        }
        entity.setScopeMode(DataScopeMode.valueOf(fields.requiredString(resource, "scopeMode")).name());
        entity.setScopeValues(writeScopeValues(scopeValues(resource, role)));
        entity.setIncludeChildren(fields.boolField(resource, "includeChildren", false));
        entity.setStatus(statusValue(resource));
        entity.setUpdateTime(now);
        if (creating) {
            roleDataScopeMapper.insert(entity);
        } else {
            roleDataScopeMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Auth role data scope synced: " + role.getRoleCode() + "/" + resourceCode);
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            return disableRegistryTarget(resource, targetId);
        }
        Long tenantId = fields.requiredLong(resource, "tenantId");
        return ResourceTenantScope.call(tenantId, () -> disableInTenant(resource));
    }

    private ResourceSyncResult disableRegistryTarget(ResourceDeclaration resource, Long targetId) {
        validateTargetTable(resource, targetId);
        RoleDataScopeEntity target = roleDataScopeMapper.selectRegistryTargetById(targetId);
        if (target == null) {
            throw invalidRegistryTarget(resource, targetId, "target does not exist");
        }
        Long tenantId = target.getTenantIdAsLong();
        if (tenantId == null) {
            throw invalidRegistryTarget(resource, targetId, "target tenantId is missing");
        }
        validateRegistryIdentity(resource, target, tenantId);
        return ResourceTenantScope.call(tenantId,
                () -> disableRegistryTargetInTenant(resource, targetId, tenantId));
    }

    private ResourceSyncResult disableRegistryTargetInTenant(ResourceDeclaration resource,
                                                              Long targetId,
                                                              Long tenantId) {
        RoleDataScopeEntity target = roleDataScopeMapper.selectById(targetId);
        if (target == null || !tenantId.equals(target.getTenantIdAsLong())) {
            throw invalidRegistryTarget(resource, targetId, "target is not visible in its tenant context");
        }
        return disableEntity(target);
    }

    private void validateTargetTable(ResourceDeclaration resource, Long targetId) {
        String targetTable = fields.stringField(resource, "targetTable");
        if (targetTable != null && !targetTable.isBlank() && !TARGET_TABLE.equals(targetTable.trim())) {
            throw invalidRegistryTarget(resource, targetId, "targetTable does not match " + TARGET_TABLE);
        }
    }

    private void validateRegistryIdentity(ResourceDeclaration resource,
                                            RoleDataScopeEntity target,
                                            Long tenantId) {
        Long declaredTenantId = fields.longField(resource, "tenantId");
        if (declaredTenantId != null && !declaredTenantId.equals(tenantId)) {
            throw invalidRegistryTarget(resource, target.getId(), "tenantId does not match target");
        }
        validateOptionalIdentity(resource, target, "appCode", target.getAppCode());
        validateOptionalIdentity(resource, target, "resourceCode", target.getResourceCode());
    }

    private void validateOptionalIdentity(ResourceDeclaration resource,
                                            RoleDataScopeEntity target,
                                            String fieldName,
                                            String targetValue) {
        String declaredValue = fields.stringField(resource, fieldName);
        if (declaredValue != null && !declaredValue.isBlank()
                && !declaredValue.trim().equals(targetValue)) {
            throw invalidRegistryTarget(resource, target.getId(), fieldName + " does not match target");
        }
    }

    private IllegalStateException invalidRegistryTarget(ResourceDeclaration resource,
                                                         Long targetId,
                                                         String reason) {
        return new IllegalStateException("AUTH_ROLE_DATA_SCOPE registry target is invalid: resourceId="
                + resource.getId() + ", targetId=" + targetId + ", reason=" + reason);
    }

    private ResourceSyncResult disableInTenant(ResourceDeclaration resource) {
        RoleEntity role = requiredRole(resource);
        RoleDataScopeEntity entity = findScope(role, fields.requiredString(resource, "resourceCode"));
        return disableEntity(entity);
    }

    private ResourceSyncResult disableEntity(RoleDataScopeEntity entity) {
        boolean changed = false;
        if (entity != null && !Integer.valueOf(0).equals(entity.getStatus())) {
            entity.setStatus(0);
            entity.setUpdateTime(LocalDateTime.now());
            changed = roleDataScopeMapper.updateById(entity) > 0;
        }
        return ResourceSyncResult.of(entity == null ? null : entity.getId(), TARGET_TABLE,
                "Auth role data scope disabled: changed=" + changed);
    }

    private RoleEntity requiredRole(ResourceDeclaration resource) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, fields.requiredLong(resource, "tenantId"))
                .eq(RoleEntity::getAppCode, fields.stringField(resource, "appCode", DEFAULT_APP_CODE))
                .eq(RoleEntity::getRoleCode, fields.requiredString(resource, "roleCode"))
                .last("LIMIT 1");
        RoleEntity role = roleMapper.selectOne(wrapper);
        if (role == null) {
            throw new IllegalStateException("AUTH_ROLE_DATA_SCOPE referenced role does not exist: "
                    + fields.requiredString(resource, "roleCode"));
        }
        return role;
    }

    private RoleDataScopeEntity findScope(RoleEntity role, String resourceCode) {
        return roleDataScopeMapper.selectOne(new LambdaQueryWrapper<RoleDataScopeEntity>()
                .eq(RoleDataScopeEntity::getTenantId, role.getTenantId())
                .eq(RoleDataScopeEntity::getAppCode, role.getAppCode())
                .eq(RoleDataScopeEntity::getRoleId, role.getRoleId())
                .eq(RoleDataScopeEntity::getResourceCode, resourceCode)
                .last("LIMIT 1"));
    }

    private String writeScopeValues(List<String> values) {
        try {
            return scopeValuesWriter.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AUTH_ROLE_DATA_SCOPE scopeValues cannot be serialized", e);
        }
    }

    private List<String> scopeValues(ResourceDeclaration resource, RoleEntity role) {
        List<String> orgCodes = fields.stringListField(resource, "orgCodes");
        if (!orgCodes.isEmpty()) {
            return orgCodes.stream()
                    .map(orgCode -> requiredOrgId(role.getTenantIdAsLong(), orgCode))
                    .map(String::valueOf)
                    .toList();
        }
        return fields.stringListField(resource, "scopeValues");
    }

    private Long requiredOrgId(Long tenantId, String orgCode) {
        AuthorizationOrgReferenceProvider provider = orgReferenceProvider.getIfAvailable();
        Long orgId = provider == null ? null : provider.resolveOrgId(tenantId, orgCode);
        if (orgId == null) {
            throw new IllegalStateException("AUTH_ROLE_DATA_SCOPE referenced org does not exist: " + orgCode);
        }
        return orgId;
    }

    private Integer statusValue(ResourceDeclaration resource) {
        Integer status = fields.intField(resource, "status", null);
        if (status != null) {
            return status;
        }
        return resource.getStatus() == ResourceStatus.DISABLED ? 0 : 1;
    }
}
