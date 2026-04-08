package io.mango.infra.security.core.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.mango.infra.security.api.ITokenService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JJWT-based implementation of {@link ITokenService}.
 *
 * @author Mango
 */
@Slf4j
public class JjwtTokenServiceImpl implements ITokenService {

    @Value("${mango.security.jwt.secret:mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits}")
    private String secret;

    @Value("${mango.security.jwt.access-token-validity:7200}")
    private long accessTokenValiditySeconds;

    @Value("${mango.security.jwt.refresh-token-validity:604800}")
    private long refreshTokenValiditySeconds;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .claim("userId", userId)
                .claim("type", TOKEN_TYPE_ACCESS)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenValiditySeconds * 1000))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("type", TOKEN_TYPE_REFRESH)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenValiditySeconds * 1000))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        return !claims.getExpiration().before(new Date());
    }

    @Override
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    @Override
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    @Override
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object type = claims.get("type");
        return type != null ? type.toString() : null;
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        Claims claims = parseToken(refreshToken);
        if (claims == null) {
            return null;
        }
        Object type = claims.get("type");
        if (!TOKEN_TYPE_REFRESH.equals(type != null ? type.toString() : null)) {
            return null;
        }
        if (claims.getExpiration().before(new Date())) {
            return null;
        }

        Long userId = getUserId(refreshToken);
        String username = getUsername(refreshToken);
        if (userId == null || username == null) {
            return null;
        }

        String newAccessToken = generateAccessToken(userId, username, Map.of());
        String newRefreshToken = generateRefreshToken(userId, username);
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }
}
