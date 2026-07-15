package io.mango.authorization.starter.controller;

import io.mango.authorization.api.PermissionApi;
import io.mango.authorization.api.command.MenuCommand;
import io.mango.authorization.core.service.IMenuService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationApiContractTest {

    @Test
    void authorizationApi_只由Controller承载() {
        assertThat(PermissionApi.class).isAssignableFrom(PermissionController.class);
        assertThat(PermissionApi.class.isAssignableFrom(IMenuService.class)).isFalse();
    }

    @Test
    void menuCommand_根菜单允许使用零作为父菜单ID() {
        MenuCommand command = new MenuCommand();
        command.setAppCode("internal-admin");
        command.setModuleCode("mango-system");
        command.setParentId(0L);
        command.setMenuType(2);
        command.setMenuName("系统设置");
        command.setMenuCode("system:settings");

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(command)).isEmpty();
        }
    }
}
