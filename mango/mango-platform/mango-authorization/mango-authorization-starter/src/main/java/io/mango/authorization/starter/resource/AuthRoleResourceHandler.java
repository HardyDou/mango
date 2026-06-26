package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Resource handler for authorization role baseline declarations.
 */
@Component
@RequiredArgsConstructor
public class AuthRoleResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_role";
    private static final String DEFAULT_APP_CODE = "internal-admin";
    private static final String DEFAULT_REALM = "INTERNAL";
    private static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";

    private final RoleMapper roleMapper;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.AUTH_ROLE);

    @Override
    public String resourceType() {
        return ResourceTypes.AUTH_ROLE;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("roleCode")
                .requiredField("roleName")
                .fieldDescription("tenantId", "租户 ID。")
                .fieldDescription("appCode", "授权应用编码，默认 internal-admin。")
                .fieldDescription("realm", "登录域，默认 INTERNAL。")
                .fieldDescription("actorType", "操作者类型，默认 INTERNAL_USER。")
                .fieldDescription("roleCode", "角色编码。")
                .fieldDescription("roleName", "角色名称。")
                .fieldDescription("roleType", "角色类型：1-系统角色，2-业务角色。")
                .fieldDescription("status", "状态：0-禁用，1-启用。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Role role = findRole(resource);
        LocalDateTime now = LocalDateTime.now();
        if (role == null) {
            role = new Role();
            role.setTenantId(fields.requiredLong(resource, "tenantId"));
            role.setAppCode(fields.stringField(resource, "appCode", DEFAULT_APP_CODE));
            role.setRealm(fields.stringField(resource, "realm", DEFAULT_REALM));
            role.setActorType(fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE));
            role.setRoleCode(fields.requiredString(resource, "roleCode"));
            role.setCreateTime(now);
        }
        role.setRoleName(fields.requiredString(resource, "roleName"));
        role.setRoleType(fields.intField(resource, "roleType", 2));
        role.setStatus(statusValue(resource));
        role.setSort(fields.intField(resource, "sort", 0));
        role.setRemark(fields.stringField(resource, "remark"));
        role.setUpdateTime(now);
        if (role.getRoleId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        return ResourceSyncResult.of(role.getRoleId(), TARGET_TABLE,
                "Auth role synced: " + role.getAppCode() + "/" + role.getRoleCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Role role = findByTargetOrBusinessKey(resource);
        boolean changed = false;
        if (role != null && !Integer.valueOf(0).equals(role.getStatus())) {
            role.setStatus(0);
            role.setUpdateTime(LocalDateTime.now());
            changed = roleMapper.updateById(role) > 0;
        }
        return ResourceSyncResult.of(role == null ? null : role.getRoleId(), TARGET_TABLE,
                "Auth role disabled: changed=" + changed);
    }

    private Role findRole(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            Role role = roleMapper.selectById(targetId);
            if (role != null) {
                return role;
            }
        }
        return findByBusinessKey(resource);
    }

    private Role findByTargetOrBusinessKey(ResourceDeclaration resource) {
        Long targetId = fields.longField(resource, "targetId");
        if (targetId != null) {
            Role role = roleMapper.selectById(targetId);
            if (role != null) {
                return role;
            }
        }
        return findByBusinessKey(resource);
    }

    private Role findByBusinessKey(ResourceDeclaration resource) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, fields.requiredLong(resource, "tenantId"))
                .eq(Role::getAppCode, fields.stringField(resource, "appCode", DEFAULT_APP_CODE))
                .eq(Role::getRealm, fields.stringField(resource, "realm", DEFAULT_REALM))
                .eq(Role::getActorType, fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE))
                .eq(Role::getRoleCode, fields.requiredString(resource, "roleCode"))
                .last("LIMIT 1");
        return roleMapper.selectOne(wrapper);
    }

    private Integer statusValue(ResourceDeclaration resource) {
        Integer status = fields.intField(resource, "status", null);
        if (status != null) {
            return status;
        }
        return resource.getStatus() == ResourceStatus.DISABLED ? 0 : 1;
    }
}
