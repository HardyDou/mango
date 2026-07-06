package io.mango.authorization.starter.controller;

import io.mango.authorization.api.PermissionApi;
import io.mango.authorization.core.service.IMenuService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationApiContractTest {

    @Test
    void authorizationApi_只由Controller承载() {
        assertThat(PermissionApi.class).isAssignableFrom(PermissionController.class);
        assertThat(PermissionApi.class.isAssignableFrom(IMenuService.class)).isFalse();
    }
}
