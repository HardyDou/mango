package io.mango.system.starter.resource;

import io.mango.i18n.api.resource.I18nMessageResourceDeclarations;
import io.mango.i18n.api.resource.I18nMessageResourceDeclarations.I18nMessageSpec;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * System module i18n resources.
 */
@Component
public class SystemI18nMessageResourceProvider implements ResourceProvider {

    @Override
    public List<String> moduleCodes() {
        return List.of("system");
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return specs().stream()
                .map(I18nMessageResourceDeclarations::message)
                .toList();
    }

    private List<I18nMessageSpec> specs() {
        return List.of(
                spec(2026061900900010000L, "common.save", "保存", "Save", "Save button"),
                spec(2026061900900010001L, "common.cancel", "取消", "Cancel", "Cancel button"),
                spec(2026061900900010002L, "common.delete", "删除", "Delete", "Delete button"),
                spec(2026061900900010003L, "common.edit", "编辑", "Edit", "Edit button"),
                spec(2026061900900010004L, "common.add", "新增", "Add", "Add button"),
                spec(2026061900900010005L, "common.search", "搜索", "Search", "Search button"),
                spec(2026061900900010006L, "common.reset", "重置", "Reset", "Reset button"),
                spec(2026061900900010007L, "common.submit", "提交", "Submit", "Submit button"),
                spec(2026061900900010008L, "common.confirm", "确认", "Confirm", "Confirm button"),
                spec(2026061900900010009L, "common.back", "返回", "Back", "Back button"),
                spec(2026061900900010010L, "menu.home", "首页", "Home", "Home menu"),
                spec(2026061900900010011L, "menu.system", "系统管理", "System", "System management menu"),
                spec(2026061900900010012L, "menu.user", "用户管理", "User Management", "User management menu"),
                spec(2026061900900010013L, "menu.role", "角色管理", "Role Management", "Role management menu"),
                spec(2026061900900010014L, "menu.permission", "权限管理", "Permission Management", "Permission management menu"),
                spec(2026061900900010015L, "menu.org", "组织管理", "Organization Management", "Organization management menu"),
                spec(2026061900900010016L, "menu.i18n", "国际化管理", "I18n Management", "I18n management menu"),
                spec(2026061900900010017L, "message.success", "操作成功", "Operation successful", "Success message"),
                spec(2026061900900010018L, "message.error", "操作失败", "Operation failed", "Error message"),
                spec(2026061900900010019L, "message.unauthorized", "未授权访问", "Unauthorized", "Unauthorized message")
        );
    }

    private I18nMessageSpec spec(long id, String name, String zhCn, String en, String description) {
        return new I18nMessageSpec(id, 1, "system", "系统管理", name, zhCn, en, description);
    }
}
