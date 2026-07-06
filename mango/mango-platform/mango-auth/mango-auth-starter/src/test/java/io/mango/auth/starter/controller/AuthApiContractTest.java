package io.mango.auth.starter.controller;

import io.mango.auth.api.AuthApi;
import io.mango.auth.core.service.IAuthService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiContractTest {

    @Test
    void authApi_只由Controller承载() {
        assertThat(AuthApi.class).isAssignableFrom(AuthController.class);
        assertThat(AuthApi.class.isAssignableFrom(IAuthService.class)).isFalse();
    }
}
