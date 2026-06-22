package io.mango.authorization.starter.resource;

import io.mango.authorization.api.AuthorizationResourceTypes;
import io.mango.authorization.core.entity.FrontendAppRegistry;
import io.mango.authorization.core.service.IAuthorizationAppService;
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

class FrontendAppRegistryResourceHandlerTest {

    private final IAuthorizationAppService appService = mock(IAuthorizationAppService.class);
    private final FrontendAppRegistryResourceHandler handler = new FrontendAppRegistryResourceHandler(appService);

    @Test
    void upsertConvertsResourceDeclarationToFrontendAppRegistry() {
        ResourceDeclaration resource = resource();
        when(appService.saveFrontendAppRegistry(org.mockito.ArgumentMatchers.any())).thenReturn(7001L);

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.FRONTEND_APP_REGISTRY);
        assertThat(handler.resourceType()).isEqualTo(AuthorizationResourceTypes.FRONTEND_APP_REGISTRY);
        assertThat(result.getTargetId()).isEqualTo(7001L);
        assertThat(result.getTargetTable()).isEqualTo("authorization_frontend_app_registry");
        ArgumentCaptor<FrontendAppRegistry> captor = ArgumentCaptor.forClass(FrontendAppRegistry.class);
        verify(appService).saveFrontendAppRegistry(captor.capture());
        FrontendAppRegistry registry = captor.getValue();
        assertThat(registry.getAppCode()).isEqualTo("guarantee-local");
        assertThat(registry.getAppType()).isEqualTo("MICRO_APP");
        assertThat(registry.getDeployMode()).isEqualTo("REMOTE");
        assertThat(registry.getEntryUrl()).isEqualTo("http://127.0.0.1:5188/src/micro.ts");
        assertThat(registry.getMountPath()).isEqualTo("/micro/guarantee");
        assertThat(registry.getActiveRule()).isEqualTo("/guarantee/**");
        assertThat(registry.getFramework()).isEqualTo("vue3");
        assertThat(registry.getVersion()).isEqualTo("dev");
        assertThat(registry.getHealthCheckUrl()).isEqualTo("/health");
        assertThat(registry.getSandboxEnabled()).isTrue();
        assertThat(registry.getStyleIsolation()).isEqualTo("SCOPED");
    }

    @Test
    void upsertRequiresAppCode() {
        ResourceDeclaration resource = resource();
        resource.getFields().remove("appCode");

        assertThatThrownBy(() -> handler.upsert(resource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FRONTEND_APP_REGISTRY field is required: appCode");
    }

    @Test
    void disableDeletesRegistryBecauseTableHasNoStatusColumn() {
        ResourceDeclaration resource = resource();
        when(appService.deleteFrontendAppRegistry("guarantee-local")).thenReturn(true);

        ResourceSyncResult result = handler.disable(resource);

        verify(appService).deleteFrontendAppRegistry("guarantee-local");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void disableCanUseTargetIdFromRegistryRow() {
        ResourceDeclaration resource = resource();
        resource.getFields().clear();
        put(resource, "targetId", ResourceFieldType.LONG, 7001L);
        when(appService.deleteFrontendAppRegistry(7001L)).thenReturn(true);

        ResourceSyncResult result = handler.disable(resource);

        verify(appService).deleteFrontendAppRegistry(7001L);
        assertThat(result.getMessage()).contains("targetId=7001");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void deleteDelegatesToAuthorizationService() {
        ResourceDeclaration resource = resource();
        when(appService.deleteFrontendAppRegistry("guarantee-local")).thenReturn(true);

        ResourceSyncResult result = handler.delete(resource);

        verify(appService).deleteFrontendAppRegistry("guarantee-local");
        assertThat(result.getMessage()).contains("deleted");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void deleteCanUseTargetIdFromRegistryRow() {
        ResourceDeclaration resource = resource();
        resource.getFields().clear();
        put(resource, "targetId", ResourceFieldType.LONG, 7001L);
        when(appService.deleteFrontendAppRegistry(7001L)).thenReturn(true);

        ResourceSyncResult result = handler.delete(resource);

        verify(appService).deleteFrontendAppRegistry(7001L);
        assertThat(result.getMessage()).contains("deleted");
        assertThat(result.getMessage()).contains("targetId=7001");
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setId("2951300000000010001");
        resource.setVersion(1);
        resource.setResourceType(AuthorizationResourceTypes.FRONTEND_APP_REGISTRY);
        resource.setModuleCode("guarantee");
        resource.setModuleName("保函业务");
        resource.setBizKey("guarantee.frontend.app.guarantee-local");
        resource.setTargetModule("authorization");
        resource.setFields(new LinkedHashMap<>());
        put(resource, "appCode", ResourceFieldType.STRING, "guarantee-local");
        put(resource, "appType", ResourceFieldType.STRING, "MICRO_APP");
        put(resource, "deployMode", ResourceFieldType.STRING, "REMOTE");
        put(resource, "entryUrl", ResourceFieldType.STRING, "http://127.0.0.1:5188/src/micro.ts");
        put(resource, "mountPath", ResourceFieldType.STRING, "/micro/guarantee");
        put(resource, "activeRule", ResourceFieldType.STRING, "/guarantee/**");
        put(resource, "framework", ResourceFieldType.STRING, "vue3");
        put(resource, "version", ResourceFieldType.STRING, "dev");
        put(resource, "healthCheckUrl", ResourceFieldType.STRING, "/health");
        put(resource, "sandboxEnabled", ResourceFieldType.BOOLEAN, true);
        put(resource, "styleIsolation", ResourceFieldType.STRING, "SCOPED");
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.getFields().put(name, field);
    }
}
