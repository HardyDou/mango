package io.mango.authorization.starter.resource;

import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.api.query.MenuPackageQuery;
import io.mango.authorization.api.vo.MenuPackageVO;
import io.mango.authorization.core.service.IMenuPackageService;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Synchronizes the menu-package master data required by AUTH_MENU package bindings.
 */
@Component
@RequiredArgsConstructor
public class AuthMenuPackageResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "authorization_menu_package";

    private final IMenuPackageService menuPackageService;
    private final ResourceFieldReader fields = new ResourceFieldReader(ResourceTypes.AUTH_MENU_PACKAGE);

    @Override
    public String resourceType() {
        return ResourceTypes.AUTH_MENU_PACKAGE;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("appCode")
                .requiredField("packageCode")
                .requiredField("packageName")
                .fieldDescription("targetId", "可选稳定套餐 ID，用于与租户初始化数据中的 packageId 建立引用。")
                .fieldDescription("packageCode", "菜单套餐稳定编码，供 AUTH_MENU packageCodes 引用。")
                .build();
    }

    @Override
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        MenuPackageVO existing = find(resource);
        MenuPackageCommand command = toCommand(resource, existing);
        Long packageId;
        if (existing == null) {
            packageId = menuPackageService.create(command);
        } else {
            menuPackageService.update(command);
            packageId = existing.getPackageId();
        }
        return ResourceSyncResult.of(packageId, TARGET_TABLE,
                "Auth menu package synced: " + command.getPackageCode());
    }

    @Override
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        MenuPackageVO existing = find(resource);
        if (existing == null) {
            return ResourceSyncResult.of(null, TARGET_TABLE, "Auth menu package disabled: changed=false");
        }
        MenuPackageCommand command = toCommand(resource, existing);
        command.setStatus(0);
        boolean changed = menuPackageService.update(command);
        return ResourceSyncResult.of(existing.getPackageId(), TARGET_TABLE,
                "Auth menu package disabled: changed=" + changed);
    }

    private MenuPackageVO find(ResourceDeclaration resource) {
        String appCode = fields.requiredString(resource, "appCode");
        String packageCode = fields.requiredString(resource, "packageCode");
        MenuPackageQuery query = new MenuPackageQuery();
        query.setAppCode(appCode);
        query.setKeyword(packageCode);
        return menuPackageService.listPackages(query).stream()
                .filter(item -> packageCode.equals(item.getPackageCode()))
                .findFirst()
                .orElse(null);
    }

    private MenuPackageCommand toCommand(ResourceDeclaration resource, MenuPackageVO existing) {
        MenuPackageCommand command = new MenuPackageCommand();
        command.setPackageId(existing == null
                ? fields.longField(resource, "targetId")
                : existing.getPackageId());
        command.setAppCode(fields.requiredString(resource, "appCode"));
        command.setPackageCode(fields.requiredString(resource, "packageCode"));
        command.setPackageName(fields.stringField(resource, "packageName",
                existing == null ? null : existing.getPackageName()));
        if (command.getPackageName() == null || command.getPackageName().isBlank()) {
            throw new IllegalStateException(ResourceTypes.AUTH_MENU_PACKAGE + " field is required: packageName");
        }
        command.setStatus(statusValue(resource, existing));
        command.setSort(fields.intField(resource, "sort",
                existing == null || existing.getSort() == null ? 0 : existing.getSort()));
        command.setRemark(fields.stringField(resource, "remark", existing == null ? null : existing.getRemark()));
        command.setMenuIds(existing == null || existing.getMenuIds() == null ? List.of() : existing.getMenuIds());
        return command;
    }

    private Integer statusValue(ResourceDeclaration resource, MenuPackageVO existing) {
        Integer status = fields.intField(resource, "status", null);
        if (status != null) {
            return status;
        }
        if (resource.getStatus() == ResourceStatus.DISABLED) {
            return 0;
        }
        return existing == null || existing.getStatus() == null ? 1 : existing.getStatus();
    }
}
