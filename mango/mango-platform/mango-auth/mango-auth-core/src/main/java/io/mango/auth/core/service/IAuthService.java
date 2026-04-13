package io.mango.auth.core.service;

import io.mango.auth.api.vo.LoginResponse;

public interface IAuthService {
    LoginResponse login(String username, String password);
    void logout(String token);
    LoginResponse refreshToken(String refreshToken);
    boolean validateToken(String token);
}
