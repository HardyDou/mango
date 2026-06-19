package io.mango.authorization.starter.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.core.service.IAppModuleService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthMenuResourceHandlerTest {

    private final IAppModuleService appModuleService = mock(IAppModuleService.class);
    private final AuthMenuResourceHandler handler =
            new AuthMenuResourceHandler(appModuleService, new ObjectMapper());

    @Test
    void upsertConvertsResourceDeclarationToManifestCommand() {
        ResourceDeclaration resource = menuResource();
        when(appModuleService.registerResourceManifest(org.mockito.ArgumentMatchers.any())).thenReturn(3);

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.AUTH_MENU);
        assertThat(result.getTargetTable()).isEqualTo("authorization_menu");
        ArgumentCaptor<AppModuleResourceManifestCommand> captor =
                ArgumentCaptor.forClass(AppModuleResourceManifestCommand.class);
        verify(appModuleService).registerResourceManifest(captor.capture());
        AppModuleResourceManifestCommand command = captor.getValue();
        assertThat(command.getAppCode()).isEqualTo("internal-admin");
        assertThat(command.getModuleCode()).isEqualTo("mango-workflow");
        assertThat(command.getModuleName()).isEqualTo("工作流");
        assertThat(command.getSort()).isEqualTo(6);
        assertThat(command.getPackageCodes()).containsExactly("internal-admin-default");
        assertThat(command.getRoleCodes()).containsExactly("ROLE_ADMIN");
        assertThat(command.getMenus()).hasSize(1);
        assertThat(command.getMenus().get(0).getMenuCode()).isEqualTo("workflow");
        assertThat(command.getMenus().get(0).getChildren()).hasSize(1);
        assertThat(command.getMenus().get(0).getChildren().get(0).getPermissionItems()).hasSize(1);
        assertThat(command.getMenus().get(0).getChildren().get(0).getPermissionItems().get(0).getMenuCode())
                .isEqualTo("workflow:task:list-button");
    }

    @Test
    void upsertRequiresMenusField() {
        ResourceDeclaration resource = menuResource();
        resource.getFields().remove("menus");

        assertThatThrownBy(() -> handler.upsert(resource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_MENU field is required: menus");
    }

    @Test
    void disableDelegatesToAppModuleService() {
        ResourceDeclaration resource = menuResource();
        when(appModuleService.disable("internal-admin", "mango-workflow")).thenReturn(true);

        ResourceSyncResult result = handler.disable(resource);

        verify(appModuleService).disable("internal-admin", "mango-workflow");
        assertThat(result.getMessage()).contains("changed=true");
    }

    @Test
    void upsertBatchOrdersResourcesByDeclaredParentCode() {
        ResourceDeclaration child = menuResource(
                "2951300000000009002",
                "workflow.child",
                "mango-workflow",
                List.of(Map.of(
                        "menuType", 2,
                        "menuName", "发起流程",
                        "menuCode", "workflow:start-process",
                        "parentCode", "workflow",
                        "path", "/workflow/start-process"
                )));
        ResourceDeclaration parent = menuResource(
                "2951300000000009001",
                "workflow.parent",
                "mango-workflow",
                List.of(Map.of(
                        "menuType", 1,
                        "menuName", "审批中心",
                        "menuCode", "workflow",
                        "path", "/workflow"
                )));
        when(appModuleService.registerResourceManifest(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        Map<String, ResourceSyncResult> results = handler.upsertBatch(List.of(child, parent));

        assertThat(results.keySet()).containsExactly(parent.getId(), child.getId());
        ArgumentCaptor<AppModuleResourceManifestCommand> captor =
                ArgumentCaptor.forClass(AppModuleResourceManifestCommand.class);
        verify(appModuleService, times(2)).registerResourceManifest(captor.capture());
        assertThat(captor.getAllValues().get(0).getMenus().get(0).getMenuCode()).isEqualTo("workflow");
        assertThat(captor.getAllValues().get(1).getMenus().get(0).getMenuCode()).isEqualTo("workflow:start-process");
    }

    @Test
    void upsertBatchAllowsParentCodeDeclaredInsideSameResource() {
        ResourceDeclaration resource = menuResource(
                "2951300000000009001",
                "workflow.menu",
                "mango-workflow",
                List.of(Map.of(
                        "menuType", 1,
                        "menuName", "审批中心",
                        "menuCode", "workflow",
                        "path", "/workflow",
                        "children", List.of(Map.of(
                                "menuType", 2,
                                "menuName", "流程任务",
                                "menuCode", "workflow:task",
                                "parentCode", "workflow",
                                "path", "/workflow/task"
                        ))
                )));
        when(appModuleService.registerResourceManifest(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        Map<String, ResourceSyncResult> results = handler.upsertBatch(List.of(resource));

        assertThat(results.keySet()).containsExactly(resource.getId());
        verify(appModuleService).registerResourceManifest(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void upsertBatchRejectsCircularDeclaredParentCode() {
        ResourceDeclaration first = menuResource(
                "2951300000000009001",
                "workflow.first",
                "mango-workflow",
                List.of(Map.of(
                        "menuType", 1,
                        "menuName", "审批中心",
                        "menuCode", "workflow",
                        "parentCode", "workflow:task",
                        "path", "/workflow"
                )));
        ResourceDeclaration second = menuResource(
                "2951300000000009002",
                "workflow.second",
                "mango-workflow",
                List.of(Map.of(
                        "menuType", 1,
                        "menuName", "流程办理",
                        "menuCode", "workflow:task",
                        "parentCode", "workflow",
                        "path", "/workflow/task"
                )));

        assertThatThrownBy(() -> handler.upsertBatch(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_MENU resources have unresolved parent dependencies");
    }

    private ResourceDeclaration menuResource() {
        return menuResource(
                "2951300000000009001",
                "workflow.menu.internal-admin",
                "mango-workflow",
                List.of(Map.of(
                        "menuType", 1,
                        "menuName", "审批中心",
                        "menuCode", "workflow",
                        "path", "/workflow",
                        "children", List.of(Map.of(
                                "menuType", 2,
                                "menuName", "发起流程",
                                "menuCode", "workflow:start-process",
                                "path", "/workflow/start-process",
                                "permissionItems", List.of(Map.of(
                                        "menuCode", "workflow:task:list-button",
                                        "permissionCode", "workflow:task:list",
                                        "permissionName", "查询流程任务",
                                        "sort", 0
                                ))
                        ))
                )));
    }

    private ResourceDeclaration menuResource(String id, String bizKey, String moduleCode, List<Map<String, Object>> menus) {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setId(id);
        resource.setVersion(1);
        resource.setResourceType(ResourceTypes.AUTH_MENU);
        resource.setModuleCode("workflow");
        resource.setModuleName("工作流");
        resource.setBizKey(bizKey);
        resource.setTargetModule("authorization");
        resource.setFields(new LinkedHashMap<>());
        put(resource, "appCode", ResourceFieldType.STRING, "internal-admin");
        put(resource, "moduleCode", ResourceFieldType.STRING, moduleCode);
        put(resource, "moduleName", ResourceFieldType.STRING, "工作流");
        put(resource, "sort", ResourceFieldType.INT, 6);
        put(resource, "packageCodes", ResourceFieldType.LIST, List.of("internal-admin-default"));
        put(resource, "roleCodes", ResourceFieldType.LIST, List.of("ROLE_ADMIN"));
        put(resource, "menus", ResourceFieldType.LIST, menus);
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.getFields().put(name, field);
    }
}
