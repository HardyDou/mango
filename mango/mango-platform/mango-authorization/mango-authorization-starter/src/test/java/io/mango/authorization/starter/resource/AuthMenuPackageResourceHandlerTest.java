package io.mango.authorization.starter.resource;

import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.api.query.MenuPackageQuery;
import io.mango.authorization.api.vo.MenuPackageVO;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthMenuPackageResourceHandlerTest {

    private final IMenuPackageService menuPackageService = mock(IMenuPackageService.class);
    private final AuthMenuPackageResourceHandler handler = new AuthMenuPackageResourceHandler(menuPackageService);

    @Test
    void upsertCreatesPackageMasterBeforeMenuBindings() {
        when(menuPackageService.listPackages(any(MenuPackageQuery.class))).thenReturn(List.of());
        when(menuPackageService.create(any())).thenReturn(11L);

        var result = handler.upsert(resource());

        ArgumentCaptor<MenuPackageCommand> captor = ArgumentCaptor.forClass(MenuPackageCommand.class);
        verify(menuPackageService).create(captor.capture());
        MenuPackageCommand command = captor.getValue();
        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.AUTH_MENU_PACKAGE);
        assertThat(result.getTargetTable()).isEqualTo("authorization_menu_package");
        assertThat(result.getTargetId()).isEqualTo(11L);
        assertThat(command.getPackageId()).isEqualTo(1L);
        assertThat(command.getPackageCode()).isEqualTo("platform_admin");
        assertThat(command.getPackageName()).isEqualTo("平台管理套餐");
        assertThat(command.getMenuIds()).isEmpty();
    }

    @Test
    void upsertKeepsExistingMenuBindingsWhenUpdatingPackageMaster() {
        MenuPackageVO existing = new MenuPackageVO();
        existing.setPackageId(11L);
        existing.setPackageCode("platform_admin");
        existing.setPackageName("旧名称");
        existing.setAppCode("internal-admin");
        existing.setStatus(1);
        existing.setMenuIds(List.of(101L, 102L));
        when(menuPackageService.listPackages(any(MenuPackageQuery.class)))
                .thenReturn(List.of(existing));
        when(menuPackageService.update(any())).thenReturn(true);

        handler.upsert(resource());

        ArgumentCaptor<MenuPackageCommand> captor = ArgumentCaptor.forClass(MenuPackageCommand.class);
        verify(menuPackageService).update(captor.capture());
        assertThat(captor.getValue().getMenuIds()).containsExactly(101L, 102L);
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.AUTH_MENU_PACKAGE);
        put(resource, "targetId", ResourceFieldType.LONG, 1L);
        put(resource, "appCode", ResourceFieldType.STRING, "internal-admin");
        put(resource, "packageCode", ResourceFieldType.STRING, "platform_admin");
        put(resource, "packageName", ResourceFieldType.STRING, "平台管理套餐");
        put(resource, "status", ResourceFieldType.INT, 1);
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.putField(name, field);
    }
}
