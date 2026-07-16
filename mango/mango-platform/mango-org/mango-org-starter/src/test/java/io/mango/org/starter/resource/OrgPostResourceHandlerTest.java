package io.mango.org.starter.resource;

import io.mango.org.core.entity.PostEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrgPostResourceHandlerTest {

    private final PostMapper postMapper = mock(PostMapper.class);
    private final OrgPostResourceHandler handler = new OrgPostResourceHandler(postMapper);

    @Test
    void upsertInsertsNewPostWhenDeclarationProvidesTargetId() {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("2951300000000009301");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.ORG_POST);
        declaration.setModuleCode("org");
        declaration.setBizKey("org.post.fixed");
        declaration.setName("固定岗位");
        declaration.setTargetModule("org");
        declaration.setFields(new LinkedHashMap<>());
        put(declaration, "targetId", ResourceFieldType.LONG, 9101L);
        put(declaration, "tenantId", ResourceFieldType.LONG, 1L);
        put(declaration, "postCode", ResourceFieldType.STRING, "FIXED_POST");
        put(declaration, "postName", ResourceFieldType.STRING, "固定岗位");
        when(postMapper.selectById(9101L)).thenReturn(null);
        when(postMapper.selectOne(any())).thenReturn(null);
        when(postMapper.insert(any(PostEntity.class))).thenReturn(1);

        ResourceSyncResult result = handler.upsert(declaration);

        ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);
        verify(postMapper).insert(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(9101L);
        assertThat(captor.getValue().getPostCode()).isEqualTo("FIXED_POST");
        assertThat(result.getTargetId()).isEqualTo(9101L);
    }

    private void put(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.putField(name, field);
    }
}
