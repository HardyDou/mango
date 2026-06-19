package io.mango.authorization.starter.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.command.AppModuleResourceManifestCommand;
import io.mango.authorization.core.service.IAppModuleService;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * mango-resource AUTH_MENU 目标处理器。
 */
@Component
@RequiredArgsConstructor
public class AuthMenuResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_menu";

    private final IAppModuleService appModuleService;
    private final ObjectMapper objectMapper;

    @Override
    public String resourceType() {
        return ResourceTypes.AUTH_MENU;
    }

    @Override
    public boolean requiresCompleteBatch() {
        return true;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("appCode")
                .requiredField("menus")
                .fieldDescription("appCode", "逻辑应用编码，例如 internal-admin。")
                .fieldDescription("moduleCode", "能力模块编码，默认取资源声明 module-code。")
                .fieldDescription("moduleName", "能力模块名称，默认取资源声明 module-name。")
                .fieldDescription("sort", "能力模块排序号。")
                .fieldDescription("packageCodes", "菜单同步到的既有套餐编码列表。")
                .fieldDescription("roleCodes", "菜单默认授权到的既有角色编码列表。")
                .fieldDescription("menus", "菜单树，结构与 AppModuleResourceManifestCommand.Menu 一致。")
                .fieldDescription("menus.packageCodes", "当前菜单同步到的既有套餐编码列表；未配置时继承父菜单或清单级 packageCodes。")
                .fieldDescription("menus.roleCodes", "当前菜单默认授权到的既有角色编码列表；未配置时继承父菜单或清单级 roleCodes。")
                .fieldDescription("menus.permissionItems.menuCode", "按钮菜单编码；为空时使用 permissionCode。")
                .fieldDescription("menus.permissionItems.packageCodes",
                        "当前按钮同步到的既有套餐编码列表；未配置时继承所属菜单 packageCodes。")
                .fieldDescription("menus.permissionItems.roleCodes",
                        "当前按钮默认授权到的既有角色编码列表；未配置时继承所属菜单 roleCodes。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        AppModuleResourceManifestCommand command = toCommand(resource);
        Integer count = appModuleService.registerResourceManifest(command);
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Auth menu synced: " + command.getAppCode() + "/" + command.getModuleCode()
                        + ", count=" + (count == null ? 0 : count));
    }

    @Override
    public Map<String, ResourceSyncResult> upsertBatch(List<ResourceDeclaration> resources) {
        List<ResourceDeclaration> pending = new ArrayList<>(resources);
        Map<String, ResourceSyncResult> results = new LinkedHashMap<>();
        Set<String> syncedMenuCodes = new HashSet<>();
        Set<String> declaredMenuCodes = collectDeclaredMenuCodes(resources);
        while (!pending.isEmpty()) {
            boolean progressed = false;
            for (int i = 0; i < pending.size(); i++) {
                ResourceDeclaration resource = pending.get(i);
                if (!dependsOnPendingParent(resource, declaredMenuCodes, syncedMenuCodes)) {
                    results.put(resource.getId(), upsert(resource));
                    collectMenuCodes(resource, syncedMenuCodes);
                    pending.remove(i);
                    i--;
                    progressed = true;
                }
            }
            if (!progressed) {
                throw new IllegalStateException("AUTH_MENU resources have unresolved parent dependencies");
            }
        }
        return results;
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        AppModuleResourceManifestCommand command = toCommand(resource);
        Boolean disabled = appModuleService.disable(command.getAppCode(), command.getModuleCode());
        return ResourceSyncResult.of(null, TARGET_TABLE,
                "Auth menu module disabled: " + command.getAppCode() + "/" + command.getModuleCode()
                        + ", changed=" + Boolean.TRUE.equals(disabled));
    }

    private AppModuleResourceManifestCommand toCommand(ResourceDeclaration resource) {
        AppModuleResourceManifestCommand command = new AppModuleResourceManifestCommand();
        command.setAppCode(requiredString(resource, "appCode"));
        command.setModuleCode(defaultString(stringField(resource, "moduleCode"), resource.getModuleCode()));
        command.setModuleName(defaultString(stringField(resource, "moduleName"), resource.getModuleName()));
        command.setStatus(intField(resource, "status"));
        command.setSort(intField(resource, "sort"));
        command.setPackageCodes(listField(resource, "packageCodes", String.class));
        command.setRoleCodes(listField(resource, "roleCodes", String.class));
        command.setMenus(menuListField(resource));
        return command;
    }

    private List<AppModuleResourceManifestCommand.Menu> menuListField(ResourceDeclaration resource) {
        Object value = fieldValue(resource, "menus");
        if (value == null) {
            throw new IllegalStateException("AUTH_MENU field is required: menus");
        }
        return objectMapper.convertValue(value,
                objectMapper.getTypeFactory().constructCollectionType(
                        List.class, AppModuleResourceManifestCommand.Menu.class));
    }

    private Set<String> collectDeclaredMenuCodes(List<ResourceDeclaration> resources) {
        Set<String> menuCodes = new HashSet<>();
        for (ResourceDeclaration resource : resources) {
            collectMenuCodes(resource, menuCodes);
        }
        return menuCodes;
    }

    private void collectMenuCodes(ResourceDeclaration resource, Set<String> menuCodes) {
        for (AppModuleResourceManifestCommand.Menu menu : menuListField(resource)) {
            collectMenuCodes(menu, menuCodes);
        }
    }

    private void collectMenuCodes(AppModuleResourceManifestCommand.Menu menu, Set<String> menuCodes) {
        if (menu == null) {
            return;
        }
        if (StringUtils.hasText(menu.getMenuCode())) {
            menuCodes.add(menu.getMenuCode().trim());
        }
        if (menu.getPermissionItems() != null) {
            for (AppModuleResourceManifestCommand.Permission permission : menu.getPermissionItems()) {
                if (permission == null) {
                    continue;
                }
                String menuCode = StringUtils.hasText(permission.getMenuCode())
                        ? permission.getMenuCode()
                        : permission.getPermissionCode();
                if (StringUtils.hasText(menuCode)) {
                    menuCodes.add(menuCode.trim());
                }
            }
        }
        if (menu.getChildren() != null) {
            for (AppModuleResourceManifestCommand.Menu child : menu.getChildren()) {
                collectMenuCodes(child, menuCodes);
            }
        }
    }

    private boolean dependsOnPendingParent(
            ResourceDeclaration resource,
            Set<String> declaredMenuCodes,
            Set<String> syncedMenuCodes) {
        Set<String> resourceMenuCodes = new HashSet<>();
        collectMenuCodes(resource, resourceMenuCodes);
        for (AppModuleResourceManifestCommand.Menu menu : menuListField(resource)) {
            if (dependsOnPendingParent(menu, declaredMenuCodes, syncedMenuCodes, resourceMenuCodes)) {
                return true;
            }
        }
        return false;
    }

    private boolean dependsOnPendingParent(
            AppModuleResourceManifestCommand.Menu menu,
            Set<String> declaredMenuCodes,
            Set<String> syncedMenuCodes,
            Set<String> resourceMenuCodes) {
        if (menu == null) {
            return false;
        }
        if (StringUtils.hasText(menu.getParentCode())) {
            String parentCode = menu.getParentCode().trim();
            if (!resourceMenuCodes.contains(parentCode)
                    && declaredMenuCodes.contains(parentCode)
                    && !syncedMenuCodes.contains(parentCode)) {
                return true;
            }
        }
        if (menu.getChildren() != null) {
            for (AppModuleResourceManifestCommand.Menu child : menu.getChildren()) {
                if (dependsOnPendingParent(child, declaredMenuCodes, syncedMenuCodes, resourceMenuCodes)) {
                    return true;
                }
            }
        }
        return false;
    }

    private <T> List<T> listField(ResourceDeclaration resource, String fieldName, Class<T> elementType) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return List.of();
        }
        return objectMapper.convertValue(value,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    private String requiredString(ResourceDeclaration resource, String fieldName) {
        String value = stringField(resource, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("AUTH_MENU field is required: " + fieldName);
        }
        return value.trim();
    }

    private String stringField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        return value == null ? null : String.valueOf(value);
    }

    private Integer intField(ResourceDeclaration resource, String fieldName) {
        Object value = fieldValue(resource, fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Object fieldValue(ResourceDeclaration resource, String fieldName) {
        ResourceField field = resource.getFields().get(fieldName);
        return field == null ? null : field.getValue();
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
