package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleDataScope;
import io.mango.authorization.core.mapper.RoleDataScopeMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRoleDataScopeResourceHandlerTest {

    private final RoleMapper roleMapper = mock(RoleMapper.class);
    private final RoleDataScopeMapper scopeMapper = mock(RoleDataScopeMapper.class);
    private final SysOrgMapper orgMapper = mock(SysOrgMapper.class);
    private final AuthRoleDataScopeResourceHandler handler =
            new AuthRoleDataScopeResourceHandler(roleMapper, scopeMapper, orgMapper, new ObjectMapper());

    @Test
    void upsertStoresScopeValuesAsJsonArray() {
        Role role = role();
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        when(scopeMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            RoleDataScope scope = invocation.getArgument(0);
            scope.setId(2001L);
            return 1;
        }).when(scopeMapper).insert(any(RoleDataScope.class));

        handler.upsert(resource());

        ArgumentCaptor<RoleDataScope> captor = ArgumentCaptor.forClass(RoleDataScope.class);
        verify(scopeMapper).insert(captor.capture());
        RoleDataScope scope = captor.getValue();
        assertThat(scope.getTenantId()).isEqualTo(1L);
        assertThat(scope.getAppCode()).isEqualTo("internal-admin");
        assertThat(scope.getRoleId()).isEqualTo(1001L);
        assertThat(scope.getResourceCode()).isEqualTo("order");
        assertThat(scope.getScopeMode()).isEqualTo("ORG");
        assertThat(scope.getScopeValues()).isEqualTo("[\"10\",\"20\"]");
        assertThat(scope.getIncludeChildren()).isTrue();
        assertThat(scope.getStatus()).isEqualTo(1);
    }

    @Test
    void upsertRejectsMissingRoleReference() {
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> handler.upsert(resource()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("referenced role does not exist");
    }

    @Test
    void upsertResolvesOrgCodesToScopeValues() {
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role());
        when(scopeMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(orgMapper.selectOne(any(Wrapper.class))).thenReturn(org(10L), org(20L));
        doAnswer(invocation -> {
            RoleDataScope scope = invocation.getArgument(0);
            scope.setId(2002L);
            return 1;
        }).when(scopeMapper).insert(any(RoleDataScope.class));
        ResourceDeclaration resource = resource();
        resource.getFields().remove("scopeValues");
        put(resource, "orgCodes", ResourceFieldType.JSON, List.of("HQ", "BRANCH"));

        handler.upsert(resource);

        ArgumentCaptor<RoleDataScope> captor = ArgumentCaptor.forClass(RoleDataScope.class);
        verify(scopeMapper).insert(captor.capture());
        assertThat(captor.getValue().getScopeValues()).isEqualTo("[\"10\",\"20\"]");
    }

    private Role role() {
        Role role = new Role();
        role.setRoleId(1001L);
        role.setTenantId(1L);
        role.setAppCode("internal-admin");
        role.setRoleCode("ROLE_DEMO");
        return role;
    }

    private SysOrg org(Long id) {
        SysOrg org = new SysOrg();
        org.setId(id);
        return org;
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.AUTH_ROLE_DATA_SCOPE);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, 1L);
        put(resource, "roleCode", ResourceFieldType.STRING, "ROLE_DEMO");
        put(resource, "resourceCode", ResourceFieldType.STRING, "order");
        put(resource, "scopeMode", ResourceFieldType.STRING, "ORG");
        put(resource, "scopeValues", ResourceFieldType.JSON, List.of("10", "20"));
        put(resource, "includeChildren", ResourceFieldType.BOOLEAN, true);
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.getFields().put(name, field);
    }
}
