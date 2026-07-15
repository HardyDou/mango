package io.mango.authorization.starter.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.command.AppCommand;
import io.mango.authorization.core.service.IAuthorizationAppService;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthAppResourceHandlerTest {

    private final IAuthorizationAppService appService = mock(IAuthorizationAppService.class);
    private final AuthAppResourceHandler handler = new AuthAppResourceHandler(appService, new ObjectMapper());

    @Test
    void upsertSynchronizesLogicalAppAndLoginContextWithoutRuntimeRegistryContract() {
        when(appService.upsertBaseline(any())).thenReturn(1L);

        var result = handler.upsert(resource());

        ArgumentCaptor<AppCommand> captor = ArgumentCaptor.forClass(AppCommand.class);
        verify(appService).upsertBaseline(captor.capture());
        AppCommand command = captor.getValue();
        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.AUTH_APP);
        assertThat(result.getTargetTable()).isEqualTo("authorization_app");
        assertThat(result.getTargetId()).isEqualTo(1L);
        assertThat(command.getAppCode()).isEqualTo("internal-admin");
        assertThat(command.getAppName()).isEqualTo("内部管理后台");
        assertThat(command.getStatus()).isEqualTo(1);
        assertThat(command.getLoginContexts()).singleElement().satisfies(context -> {
            assertThat(context.getRealm()).isEqualTo("INTERNAL");
            assertThat(context.getActorType()).isEqualTo("INTERNAL_USER");
            assertThat(context.getDefaultFlag()).isEqualTo(1);
        });
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.AUTH_APP);
        put(resource, "appCode", ResourceFieldType.STRING, "internal-admin");
        put(resource, "appName", ResourceFieldType.STRING, "内部管理后台");
        put(resource, "status", ResourceFieldType.INT, 1);
        put(resource, "loginContexts", ResourceFieldType.LIST, List.of(Map.of(
                "realm", "INTERNAL",
                "actorType", "INTERNAL_USER",
                "defaultFlag", 1,
                "status", 1,
                "sort", 0)));
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.getFields().put(name, field);
    }
}
