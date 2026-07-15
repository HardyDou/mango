package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorizationOrgReferenceProvider;
import io.mango.authorization.api.enums.DataScopeMode;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleDataScopeEntity;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resource handler for role data scope declarations.
 */
@Component
@RequiredArgsConstructor
public class AuthRoleDataScopeResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_role_data_scope";
    private static final String DEFAULT_APP_CODE = "internal-admin";

    private final RoleMapper roleMapper;
    private final RoleDataScopeMapper roleDataScopeMapper;
    private final ObjectProvider<AuthorizationOrgReferenceProvider> orgReferenceProvider;
    private final ObjectMapper objectMapper;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.AUTH_ROLE_DATA_SCOPE);

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
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            entity = new RoleDataScopeEntity();
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
        if (entity.getId() == null) {
            roleDataScopeMapper.insert(entity);
        } else {
            roleDataScopeMapper.updateById(entity);
        }
        return ResourceSyncResult.of(entity.getId(), TARGET_TABLE,
                "Auth role data scope synced: " + role.getRoleCode() + "/" + resourceCode);
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        return ResourceTenantScope.call(tenantId, () -> disableInTenant(resource));
    }

    private ResourceSyncResult disableInTenant(ResourceDeclaration resource) {
        RoleEntity role = requiredRole(resource);
        RoleDataScopeEntity entity = findScope(role, fields.requiredString(resource, "resourceCode"));
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
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
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
