package io.mango.auth.core.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.mango.auth.api.vo.LoginRequest;
import io.mango.auth.api.vo.LoginResponse;
import io.mango.auth.api.vo.SysRoleVO;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.ISysRoleService;
import io.mango.permission.api.po.SysUser;
import io.mango.permission.api.SysUserApi;
import io.mango.permission.api.SysMenuApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Authentication service implementation
 *
 * @author Mango
 */
@Slf4j
@Service
public class AuthServiceImpl implements IAuthService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final SysUserApi sysUserApi;
    private final ISysRoleService sysRoleService;
    private final SysMenuApi sysMenuApi;

    public AuthServiceImpl(SysUserApi sysUserApi, ISysRoleService sysRoleService, SysMenuApi sysMenuApi) {
        this.sysUserApi = sysUserApi;
        this.sysRoleService = sysRoleService;
        this.sysMenuApi = sysMenuApi;
    }

    @Value("${mango.jwt.secret:mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits}")
    private String jwtSecret;

    @Value("${mango.jwt.access-token-validity:7200}")
    private Long accessTokenValidity; // seconds, default 2 hours

    @Value("${mango.jwt.refresh-token-validity:604800}")
    private Long refreshTokenValidity; // seconds, default 7 days

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 1. Validate credentials (use ForAuth method to get password)
        SysUser user = sysUserApi.getByUsernameForAuth(loginRequest.getUsername());
        if (user == null) {
            log.warn("Login failed: user not found - {}", loginRequest.getUsername());
            return null;
        }

        // 2. Verify password using BCrypt
        if (!matchesPassword(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for user - {}", loginRequest.getUsername());
            return null;
        }

        // 3. Check user status
        if (user.getStatus() != 1) {
            log.warn("Login failed: user is disabled - {}", loginRequest.getUsername());
            return null;
        }

        // 4. Generate tokens
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        // 5. Build response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(accessTokenValidity);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        // 6. Load roles and permissions
        List<SysRoleVO> userRoles = sysRoleService.getUserRoles(user.getUserId());
        if (userRoles != null && !userRoles.isEmpty()) {
            response.setRoles(userRoles.stream().map(SysRoleVO::getRoleCode).collect(Collectors.toList()));
        } else {
            response.setRoles(List.of());
        }
        // Load permissions from permission service via SysMenuApi
        Set<String> userPermissions = sysMenuApi.getUserPermissions(user.getUserId());
        response.setPermissions(userPermissions != null ? List.copyOf(userPermissions) : List.of());

        log.info("User logged in successfully: {}", loginRequest.getUsername());
        return response;
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // 1. Validate refresh token
        if (!validateToken(refreshToken)) {
            log.warn("Refresh token is invalid");
            return null;
        }

        // 2. Verify token type is refresh
        String tokenType = getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            log.warn("Token type mismatch: expected 'refresh' but got '{}'", tokenType);
            return null;
        }

        // 3. Get user ID from refresh token
        Long userId = getUserIdFromToken(refreshToken);
        if (userId == null) {
            log.warn("Could not get user ID from refresh token");
            return null;
        }

        // 3. Get user with password (ForAuth method)
        SysUser user = sysUserApi.getByIdForAuth(userId);
        if (user == null || user.getStatus() != 1) {
            log.warn("User not found or disabled: {}", userId);
            return null;
        }

        // 4. Generate new tokens
        String newAccessToken = generateAccessToken(user);
        String newRefreshToken = generateRefreshToken(user);

        // 5. Build response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setExpiresIn(accessTokenValidity);
        response.setTokenType("Bearer");
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        // 6. Load roles and permissions for refresh token response
        List<SysRoleVO> userRoles = sysRoleService.getUserRoles(user.getUserId());
        if (userRoles != null && !userRoles.isEmpty()) {
            response.setRoles(userRoles.stream().map(SysRoleVO::getRoleCode).collect(Collectors.toList()));
        } else {
            response.setRoles(List.of());
        }
        Set<String> userPermissions = sysMenuApi.getUserPermissions(user.getUserId());
        response.setPermissions(userPermissions != null ? List.copyOf(userPermissions) : List.of());

        return response;
    }

    @Override
    public void logout(String token) {
        // In a real implementation, you might want to:
        // 1. Add the token to a blacklist
        // 2. Or remove from whitelist in Redis
        // For simplicity, we just log the logout
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = getUserIdFromToken(token);
        log.info("User logged out: userId={}", userId);
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to get user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get token type from token
     */
    private String getTokenType(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object type = claims.get("type");
            return type != null ? type.toString() : null;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to get token type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate access token
     */
    private String generateAccessToken(SysUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("type", "access");
        return generateToken(user.getUserId().toString(), claims, accessTokenValidity);
    }

    /**
     * Generate refresh token
     */
    private String generateRefreshToken(SysUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("type", "refresh");
        return generateToken(user.getUserId().toString(), claims, refreshTokenValidity);
    }

    /**
     * Generate JWT token
     */
    private String generateToken(String subject, Map<String, Object> claims, Long validity) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TimeUnit.SECONDS.toMillis(validity));

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Match password using BCrypt
     */
    private boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * Encode password (for initial password setup)
     */
    public String encodePassword(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }
}
