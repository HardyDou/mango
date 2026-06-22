package io.mango.authorization.starter.resource;

import io.mango.authorization.api.AuthorizationResourceTypes;
import io.mango.authorization.api.command.FrontendModuleRuntimeStrategyCommand;
import io.mango.authorization.core.service.IFrontendRuntimeStrategyService;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrontendModuleRuntimeStrategyResourceHandlerTest {

    private final IFrontendRuntimeStrategyService runtimeStrategyService =
            mock(IFrontendRuntimeStrategyService.class);
    private final FrontendModuleRuntimeStrategyResourceHandler handler =
            new FrontendModuleRuntimeStrategyResourceHandler(runtimeStrategyService);

    @Test
    void upsertConvertsResourceDeclarationToRuntimeStrategyCommand() {
        ResourceDeclaration resource = resource();
        when(runtimeStrategyService.save(org.mockito.ArgumentMatchers.any())).thenReturn(8001L);

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY);
        assertThat(handler.resourceType()).isEqualTo(AuthorizationResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY);
        assertThat(result.getTargetId()).isEqualTo(8001L);
        assertThat(result.getTargetTable()).isEqualTo("authorization_frontend_module_runtime_strategy");
        ArgumentCaptor<FrontendModuleRuntimeStrategyCommand> captor =
                ArgumentCaptor.forClass(FrontendModuleRuntimeStrategyCommand.class);
        verify(runtimeStrategyService).save(captor.capture());
        FrontendModuleRuntimeStrategyCommand command = captor.getValue();
        assertThat(command.getAppCode()).isEqualTo("internal-admin");
        assertThat(command.getModuleCode()).isEqualTo("guarantee");
        assertThat(command.getDeployProfile()).isEqualTo("hybrid");
        assertThat(command.getPageType()).isEqualTo("MICRO_ROUTE");
        assertThat(command.getRuntimeCode()).isEqualTo("guarantee-local");
        assertThat(command.getStatus()).isEqualTo(1);
        assertThat(command.getSort()).isEqualTo(30);
    }

    @Test
    void upsertFallsBackToDeclarationModuleCode() {
        ResourceDeclaration resource = resource();
        resource.getFields().remove("moduleCode");
        when(runtimeStrategyService.save(org.mockito.ArgumentMatchers.any())).thenReturn(8001L);

        handler.upsert(resource);

        ArgumentCaptor<FrontendModuleRuntimeStrategyCommand> captor =
                ArgumentCaptor.forClass(FrontendModuleRuntimeStrategyCommand.class);
        verify(runtimeStrategyService).save(captor.capture());
        assertThat(captor.getValue().getModuleCode()).isEqualTo("guarantee");
    }

    @Test
    void upsertRequiresRuntimeCode() {
        ResourceDeclaration resource = resource();
        resource.getFields().remove("runtimeCode");

        assertThatThrownBy(() -> handler.upsert(resource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FRONTEND_MODULE_RUNTIME_STRATEGY field is required: runtimeCode");
    }

    @Test
    void disableDelegatesToRuntimeStrategyService() {
        ResourceDeclaration resource = resource();
        when(runtimeStrategyService.disable("internal-admin", "guarantee", "hybrid")).thenReturn(true);

        ResourceSyncResult result = handler.disable(resource);

        verify(runtimeStrategyService).disable("internal-admin", "guarantee", "hybrid");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void disableOnlyRequiresStrategyKeyFields() {
        ResourceDeclaration resource = resource();
        resource.getFields().remove("pageType");
        resource.getFields().remove("runtimeCode");
        when(runtimeStrategyService.disable("internal-admin", "guarantee", "hybrid")).thenReturn(true);

        ResourceSyncResult result = handler.disable(resource);

        verify(runtimeStrategyService).disable("internal-admin", "guarantee", "hybrid");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void disableCanUseTargetIdFromRegistryRow() {
        ResourceDeclaration resource = resource();
        resource.getFields().clear();
        put(resource, "targetId", ResourceFieldType.LONG, 8001L);
        when(runtimeStrategyService.disable(8001L)).thenReturn(true);

        ResourceSyncResult result = handler.disable(resource);

        verify(runtimeStrategyService).disable(8001L);
        assertThat(result.getMessage()).contains("targetId=8001");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void deleteDelegatesToRuntimeStrategyService() {
        ResourceDeclaration resource = resource();
        when(runtimeStrategyService.delete("internal-admin", "guarantee", "hybrid")).thenReturn(true);

        ResourceSyncResult result = handler.delete(resource);

        verify(runtimeStrategyService).delete("internal-admin", "guarantee", "hybrid");
        assertThat(result.getMessage()).contains("deleted");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void deleteCanUseTargetIdFromRegistryRow() {
        ResourceDeclaration resource = resource();
        resource.getFields().clear();
        put(resource, "targetId", ResourceFieldType.LONG, 8001L);
        when(runtimeStrategyService.delete(8001L)).thenReturn(true);

        ResourceSyncResult result = handler.delete(resource);

        verify(runtimeStrategyService).delete(8001L);
        assertThat(result.getMessage()).contains("deleted");
        assertThat(result.getMessage()).contains("targetId=8001");
    }

    @Test
    void deleteOnlyRequiresStrategyKeyFields() {
        ResourceDeclaration resource = resource();
        resource.getFields().remove("pageType");
        resource.getFields().remove("runtimeCode");
        when(runtimeStrategyService.delete("internal-admin", "guarantee", "hybrid")).thenReturn(true);

        ResourceSyncResult result = handler.delete(resource);

        verify(runtimeStrategyService).delete("internal-admin", "guarantee", "hybrid");
        assertThat(result.getMessage()).contains("deleted");
        assertThat(result.getMessage()).contains("changed=true");
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setId("2951300000000010002");
        resource.setVersion(1);
        resource.setResourceType(AuthorizationResourceTypes.FRONTEND_MODULE_RUNTIME_STRATEGY);
        resource.setModuleCode("guarantee");
        resource.setModuleName("保函业务");
        resource.setBizKey("guarantee.frontend.strategy.internal-admin.hybrid");
        resource.setTargetModule("authorization");
        resource.setFields(new LinkedHashMap<>());
        put(resource, "appCode", ResourceFieldType.STRING, "internal-admin");
        put(resource, "moduleCode", ResourceFieldType.STRING, "guarantee");
        put(resource, "deployProfile", ResourceFieldType.STRING, "hybrid");
        put(resource, "pageType", ResourceFieldType.STRING, "MICRO_ROUTE");
        put(resource, "runtimeCode", ResourceFieldType.STRING, "guarantee-local");
        put(resource, "status", ResourceFieldType.INT, 1);
        put(resource, "sort", ResourceFieldType.INT, 30);
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.getFields().put(name, field);
    }
}
