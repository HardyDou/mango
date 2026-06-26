package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRoleResourceHandlerTest {

    private final RoleMapper roleMapper = mock(RoleMapper.class);
    private final AuthRoleResourceHandler handler = new AuthRoleResourceHandler(roleMapper);

    @Test
    void upsertCreatesRoleFromDeclaration() {
        ResourceDeclaration resource = resource();
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setRoleId(1001L);
            return 1;
        }).when(roleMapper).insert(any(Role.class));

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.AUTH_ROLE);
        assertThat(result.getTargetId()).isEqualTo(1001L);
        assertThat(result.getTargetTable()).isEqualTo("authorization_role");
        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleMapper).insert(captor.capture());
        Role role = captor.getValue();
        assertThat(role.getTenantId()).isEqualTo(1L);
        assertThat(role.getAppCode()).isEqualTo("internal-admin");
        assertThat(role.getRealm()).isEqualTo("INTERNAL");
        assertThat(role.getActorType()).isEqualTo("INTERNAL_USER");
        assertThat(role.getRoleCode()).isEqualTo("ROLE_DEMO");
        assertThat(role.getRoleName()).isEqualTo("Demo Role");
        assertThat(role.getRoleType()).isEqualTo(2);
        assertThat(role.getStatus()).isEqualTo(1);
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.AUTH_ROLE);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, 1L);
        put(resource, "roleCode", ResourceFieldType.STRING, "ROLE_DEMO");
        put(resource, "roleName", ResourceFieldType.STRING, "Demo Role");
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.getFields().put(name, field);
    }
}
