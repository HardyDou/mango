package io.mango.org.starter.resource;

import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrgUnitResourceHandlerTest {

    private final SysOrgMapper orgMapper = mock(SysOrgMapper.class);
    private final OrgUnitResourceHandler handler = new OrgUnitResourceHandler(orgMapper);

    @Test
    void upsertBatchOrdersChildAfterDeclaredParent() {
        ResourceDeclaration child = orgUnit("2951300000000009102", "org.child", "DEPT", "部门", "ROOT");
        ResourceDeclaration parent = orgUnit("2951300000000009101", "org.parent", "ROOT", "总部", null);
        SysOrg parentEntity = new SysOrg();
        parentEntity.setId(100L);
        parentEntity.setTenantId(1L);
        parentEntity.setOrgCode("ROOT");
        when(orgMapper.selectOne(any())).thenReturn(null, null, parentEntity);
        when(orgMapper.insert(any(SysOrg.class))).thenReturn(1);

        Map<String, ResourceSyncResult> results = handler.upsertBatch(List.of(child, parent));

        assertThat(results.keySet()).containsExactly(parent.getId(), child.getId());
        ArgumentCaptor<SysOrg> captor = ArgumentCaptor.forClass(SysOrg.class);
        verify(orgMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues().get(0).getOrgCode()).isEqualTo("ROOT");
        assertThat(captor.getAllValues().get(1).getOrgCode()).isEqualTo("DEPT");
        assertThat(captor.getAllValues().get(1).getPid()).isEqualTo(100L);
    }

    private ResourceDeclaration orgUnit(String id, String bizKey, String orgCode, String orgName, String parentOrgCode) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.ORG_UNIT);
        declaration.setModuleCode("org");
        declaration.setBizKey(bizKey);
        declaration.setName(orgName);
        declaration.setTargetModule("org");
        declaration.setFields(new LinkedHashMap<>());
        put(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        put(declaration, "orgCode", ResourceFieldType.STRING, orgCode);
        put(declaration, "orgName", ResourceFieldType.STRING, orgName);
        put(declaration, "orgType", ResourceFieldType.INT, 3);
        if (parentOrgCode != null) {
            put(declaration, "parentOrgCode", ResourceFieldType.STRING, parentOrgCode);
        }
        return declaration;
    }

    private void put(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }
}
