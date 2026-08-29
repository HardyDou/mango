package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationSubjectReferenceProvider;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.SubjectRoleBindingEntity;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.support.AuthorizationResourceIds;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Resource handler for subject role bootstrap bindings.
 */
@Component
@RequiredArgsConstructor
public class AuthSubjectRoleResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_subject_role";
    private static final String DEFAULT_APP_CODE = "internal-admin";
    private static final String DEFAULT_REALM = "INTERNAL";
    private static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";
    private static final String DEFAULT_SUBJECT_TYPE = "TENANT_MEMBER";

    private final RoleMapper roleMapper;
    private final SubjectRoleBindingMapper bindingMapper;
    private final ObjectProvider<AuthorizationSubjectReferenceProvider> subjectReferenceProvider;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.AUTH_SUBJECT_ROLE);

    @Override
    public String resourceType() {
        return ResourceTypes.AUTH_SUBJECT_ROLE;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(ResourceTypes.IDENTITY_USER, ResourceTypes.AUTH_ROLE);
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("roleCodes")
                .fieldDescription("subjectId", "主体 ID。subjectId、subjectCode、memberNo、username 四选一。")
                .fieldDescription("subjectCode", "主体编码，按租户成员 memberNo 解析。")
                .fieldDescription("memberNo", "租户成员编号。")
                .fieldDescription("username", "用户名，先解析 identity_user，再解析同租户成员。")
                .fieldDescription("subjectType", "主体类型，默认 TENANT_MEMBER。")
                .fieldDescription("roleCodes", "要确保绑定的角色编码列表。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        return ResourceTenantScope.call(tenantId, () -> upsertInTenant(resource, tenantId));
    }

    private ResourceSyncResult upsertInTenant(ResourceDeclaration resource, Long tenantId) {
        Long subjectId = requiredSubjectId(resource, tenantId);
        String subjectType = fields.stringField(resource, "subjectType", DEFAULT_SUBJECT_TYPE);
        List<String> roleCodes = fields.stringListField(resource, "roleCodes");
        if (roleCodes.isEmpty()) {
            throw new IllegalStateException("AUTH_SUBJECT_ROLE field is required: roleCodes");
        }
        Long firstBindingId = null;
        for (String roleCode : roleCodes) {
            RoleEntity role = requiredRole(resource, tenantId, roleCode);
            SubjectRoleBindingEntity binding = ensureBinding(
                    resource, tenantId, subjectId, subjectType, role.getRoleId(), roleCodes.size() == 1);
            if (firstBindingId == null) {
                firstBindingId = binding.getId();
            }
        }
        return ResourceSyncResult.of(firstBindingId, TARGET_TABLE,
                "Auth subject roles synced: subjectId=" + subjectId + ", count=" + roleCodes.size());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        return ResourceTenantScope.call(tenantId, () -> disableInTenant(resource, tenantId));
    }

    private ResourceSyncResult disableInTenant(ResourceDeclaration resource, Long tenantId) {
        Long subjectId = requiredSubjectId(resource, tenantId);
        String subjectType = fields.stringField(resource, "subjectType", DEFAULT_SUBJECT_TYPE);
        List<String> roleCodes = fields.stringListField(resource, "roleCodes");
        int changed = 0;
        for (String roleCode : roleCodes) {
            RoleEntity role = requiredRole(resource, tenantId, roleCode);
            changed += bindingMapper.delete(bindingWrapper(resource, tenantId, subjectId, subjectType, role.getRoleId()));
        }
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Auth subject roles disabled: subjectId=" + subjectId + ", changed=" + changed);
    }

    private Long requiredSubjectId(ResourceDeclaration resource, Long tenantId) {
        Long subjectId = fields.longField(resource, "subjectId");
        if (subjectId != null) {
            return subjectId;
        }
        AuthorizationSubjectReferenceProvider provider = subjectReferenceProvider.getIfAvailable();
        String memberNo = firstText(fields.stringField(resource, "subjectCode"),
                fields.stringField(resource, "memberNo"));
        String username = fields.stringField(resource, "username");
        Long resolved = provider == null ? null : provider.resolveMemberId(tenantId, memberNo, username);
        if (resolved != null) {
            return resolved;
        }
        throw new IllegalStateException("AUTH_SUBJECT_ROLE referenced subject does not exist");
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private SubjectRoleBindingEntity ensureBinding(ResourceDeclaration resource, Long tenantId, Long subjectId,
                                             String subjectType, Long roleId, boolean useDeclaredTargetId) {
        SubjectRoleBindingEntity existing = bindingMapper.selectOne(
                bindingWrapper(resource, tenantId, subjectId, subjectType, roleId).last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        SubjectRoleBindingEntity binding = new SubjectRoleBindingEntity();
        Long declaredTargetId = useDeclaredTargetId ? fields.longField(resource, "targetId") : null;
        binding.setId(AuthorizationResourceIds.declaredOrStable(declaredTargetId, TARGET_TABLE,
                tenantId, subjectId, subjectType,
                fields.stringField(resource, "appCode", DEFAULT_APP_CODE),
                fields.stringField(resource, "realm", DEFAULT_REALM),
                fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE),
                fields.stringField(resource, "partyType"),
                fields.longField(resource, "partyId"), roleId));
        binding.setTenantId(tenantId);
        binding.setSubjectId(subjectId);
        binding.setSubjectType(subjectType);
        binding.setAppCode(fields.stringField(resource, "appCode", DEFAULT_APP_CODE));
        binding.setRealm(fields.stringField(resource, "realm", DEFAULT_REALM));
        binding.setActorType(fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE));
        binding.setPartyType(fields.stringField(resource, "partyType"));
        binding.setPartyId(fields.longField(resource, "partyId"));
        binding.setRoleId(roleId);
        bindingMapper.insert(binding);
        return binding;
    }

    private LambdaQueryWrapper<SubjectRoleBindingEntity> bindingWrapper(ResourceDeclaration resource, Long tenantId,
                                                                  Long subjectId, String subjectType, Long roleId) {
        return new LambdaQueryWrapper<SubjectRoleBindingEntity>()
                .eq(SubjectRoleBindingEntity::getTenantId, tenantId)
                .eq(SubjectRoleBindingEntity::getSubjectId, subjectId)
                .eq(SubjectRoleBindingEntity::getSubjectType, subjectType)
                .eq(SubjectRoleBindingEntity::getAppCode, fields.stringField(resource, "appCode", DEFAULT_APP_CODE))
                .eq(SubjectRoleBindingEntity::getRealm, fields.stringField(resource, "realm", DEFAULT_REALM))
                .eq(SubjectRoleBindingEntity::getActorType, fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE))
                .eq(fields.stringField(resource, "partyType") != null,
                        SubjectRoleBindingEntity::getPartyType, fields.stringField(resource, "partyType"))
                .eq(fields.longField(resource, "partyId") != null,
                        SubjectRoleBindingEntity::getPartyId, fields.longField(resource, "partyId"))
                .eq(SubjectRoleBindingEntity::getRoleId, roleId);
    }

    private RoleEntity requiredRole(ResourceDeclaration resource, Long tenantId, String roleCode) {
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, tenantId)
                .eq(RoleEntity::getAppCode, fields.stringField(resource, "appCode", DEFAULT_APP_CODE))
                .eq(RoleEntity::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new IllegalStateException("AUTH_SUBJECT_ROLE referenced role does not exist: " + roleCode);
        }
        return role;
    }
}
