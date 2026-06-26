package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.AUTH_SUBJECT_ROLE);

    @Override
    public String resourceType() {
        return ResourceTypes.AUTH_SUBJECT_ROLE;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("subjectId")
                .requiredField("roleCodes")
                .fieldDescription("subjectId", "主体 ID，第一版不按 subjectCode 猜测解析。")
                .fieldDescription("subjectType", "主体类型，默认 TENANT_MEMBER。")
                .fieldDescription("roleCodes", "要确保绑定的角色编码列表。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Long tenantId = fields.requiredLong(resource, "tenantId");
        Long subjectId = fields.requiredLong(resource, "subjectId");
        String subjectType = fields.stringField(resource, "subjectType", DEFAULT_SUBJECT_TYPE);
        List<String> roleCodes = fields.stringListField(resource, "roleCodes");
        if (roleCodes.isEmpty()) {
            throw new IllegalStateException("AUTH_SUBJECT_ROLE field is required: roleCodes");
        }
        Long firstBindingId = null;
        for (String roleCode : roleCodes) {
            Role role = requiredRole(resource, tenantId, roleCode);
            SubjectRoleBinding binding = ensureBinding(resource, tenantId, subjectId, subjectType, role.getRoleId());
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
        Long subjectId = fields.requiredLong(resource, "subjectId");
        String subjectType = fields.stringField(resource, "subjectType", DEFAULT_SUBJECT_TYPE);
        List<String> roleCodes = fields.stringListField(resource, "roleCodes");
        int changed = 0;
        for (String roleCode : roleCodes) {
            Role role = requiredRole(resource, tenantId, roleCode);
            changed += bindingMapper.delete(bindingWrapper(resource, tenantId, subjectId, subjectType, role.getRoleId()));
        }
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Auth subject roles disabled: subjectId=" + subjectId + ", changed=" + changed);
    }

    private SubjectRoleBinding ensureBinding(ResourceDeclaration resource, Long tenantId, Long subjectId,
                                             String subjectType, Long roleId) {
        SubjectRoleBinding existing = bindingMapper.selectOne(
                bindingWrapper(resource, tenantId, subjectId, subjectType, roleId).last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        SubjectRoleBinding binding = new SubjectRoleBinding();
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

    private LambdaQueryWrapper<SubjectRoleBinding> bindingWrapper(ResourceDeclaration resource, Long tenantId,
                                                                  Long subjectId, String subjectType, Long roleId) {
        return new LambdaQueryWrapper<SubjectRoleBinding>()
                .eq(SubjectRoleBinding::getTenantId, tenantId)
                .eq(SubjectRoleBinding::getSubjectId, subjectId)
                .eq(SubjectRoleBinding::getSubjectType, subjectType)
                .eq(SubjectRoleBinding::getAppCode, fields.stringField(resource, "appCode", DEFAULT_APP_CODE))
                .eq(SubjectRoleBinding::getRealm, fields.stringField(resource, "realm", DEFAULT_REALM))
                .eq(SubjectRoleBinding::getActorType, fields.stringField(resource, "actorType", DEFAULT_ACTOR_TYPE))
                .eq(fields.stringField(resource, "partyType") != null,
                        SubjectRoleBinding::getPartyType, fields.stringField(resource, "partyType"))
                .eq(fields.longField(resource, "partyId") != null,
                        SubjectRoleBinding::getPartyId, fields.longField(resource, "partyId"))
                .eq(SubjectRoleBinding::getRoleId, roleId);
    }

    private Role requiredRole(ResourceDeclaration resource, Long tenantId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getAppCode, fields.stringField(resource, "appCode", DEFAULT_APP_CODE))
                .eq(Role::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new IllegalStateException("AUTH_SUBJECT_ROLE referenced role does not exist: " + roleCode);
        }
        return role;
    }
}
