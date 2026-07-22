package io.mango.authorization.starter.resource;

import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.core.entity.ApiResourceEntity;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.authorization.core.service.IApiResourceService;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceSyncMode;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiResourceHandlerTest {

    @Test
    void autoBatchUsesOneBulkReadForAllTargetIds() {
        IApiResourceService service = mock(IApiResourceService.class);
        ApiResourceMapper mapper = mock(ApiResourceMapper.class);
        ApiResourceHandler handler = new ApiResourceHandler(service, mapper);
        List<ResourceDeclaration> declarations = new ArrayList<>();
        List<ApiResourceEntity> entities = new ArrayList<>();
        for (int index = 0; index < 980; index++) {
            declarations.add(declaration(index, ResourceSyncMode.AUTO));
            entities.add(entity(index));
        }
        when(service.registerApiResources(any())).thenReturn(new ApiResourceRegisterResultVO(980, 980, 0));
        when(mapper.selectList(any())).thenReturn(entities);

        var results = handler.upsertBatch(declarations);

        assertThat(results).hasSize(980);
        assertThat(results.get("3000000000000000000").getTargetId()).isEqualTo(4000000000000000000L);
        assertThat(results.get("3000000000000000979").getTargetId()).isEqualTo(4000000000000000979L);
        verify(mapper, times(1)).selectList(any());
        verify(mapper, never()).selectOne(any());
    }

    @Test
    void protectedBatchRestoresExistingRowsAndStillBulkReadsTargets() {
        IApiResourceService service = mock(IApiResourceService.class);
        ApiResourceMapper mapper = mock(ApiResourceMapper.class);
        ApiResourceHandler handler = new ApiResourceHandler(service, mapper);
        ResourceDeclaration manual = declaration(1, ResourceSyncMode.MANUAL);
        ResourceDeclaration initOnly = declaration(2, ResourceSyncMode.INIT_ONLY);
        List<ApiResourceEntity> entities = List.of(entity(1), entity(2));
        when(service.registerApiResources(any())).thenReturn(new ApiResourceRegisterResultVO(2, 0, 2));
        when(mapper.selectList(any())).thenReturn(entities);

        var results = handler.upsertBatch(List.of(manual, initOnly));

        assertThat(results.values()).extracting(result -> result.getTargetId())
                .containsExactly(4000000000000000001L, 4000000000000000002L);
        verify(mapper, times(2)).selectList(any());
        verify(mapper, times(2)).updateById(any(ApiResourceEntity.class));
        verify(mapper, never()).selectOne(any());
    }

    private ResourceDeclaration declaration(int index, ResourceSyncMode syncMode) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(String.valueOf(3000000000000000000L + index));
        declaration.setVersion(1);
        declaration.setResourceType("API_RESOURCE");
        declaration.setModuleCode("authorization");
        declaration.setBizKey("authorization.api." + index);
        declaration.setTargetModule("authorization");
        declaration.setSyncMode(syncMode);
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "moduleName", "module-" + index % 10);
        field(declaration, "httpMethod", "GET");
        field(declaration, "pathPattern", "/test/" + index);
        return declaration;
    }

    private ApiResourceEntity entity(int index) {
        ApiResourceEntity entity = new ApiResourceEntity();
        entity.setId(4000000000000000000L + index);
        entity.setModuleName("module-" + index % 10);
        entity.setHttpMethod("GET");
        entity.setPathPattern("/test/" + index);
        return entity;
    }

    private void field(ResourceDeclaration declaration, String name, String value) {
        ResourceField field = new ResourceField();
        field.setType(ResourceFieldType.STRING);
        field.setValue(value);
        declaration.putField(name, field);
    }
}
