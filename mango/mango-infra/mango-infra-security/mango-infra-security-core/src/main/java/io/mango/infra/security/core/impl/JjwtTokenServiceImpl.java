package io.mango.infra.security.core.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.mango.dal.api.IKvStore;
import io.mango.infra.security.api.ITokenService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JJWT-based implementation of {@link ITokenService}.
 *
 * @author Mango
 */
@Slf4j
@Component
public class JjwtTokenServiceImpl implements ITokenService {

    private static final String LEGACY_SECRET_PROP = "mango.jwt.secret";
    private static final String DEFAULT_SECRET = "mango-secret-key-for-jwt-token-generation-must-be-at-least-256-bits";
    private static final String BLACKLIST_PREFIX = "jwt:refresh:jti:";

    private final IKvStore kvStore;
    private SecretKey secretKey;

    @Value("${mango.security.jwt.secret:}")
    private String newSecret;

    @Value("${mango.jwt.secret:}")
    private String legacySecret;

    @Value("${mango.security.jwt.access-token-validity:7200}")
    private long accessTokenValiditySeconds;

    @Value("${mango.security.jwt.refresh-token-validity:604800}")
    private long refreshTokenValiditySeconds;

    public JjwtTokenServiceImpl(@Autowired(required = false) IKvStore kvStore) {
        this.kvStore = kvStore;
    }

    @PostConstruct
    public void init() {
        // Support both new path (mango.security.jwt.secret) and legacy path (mango.jwt.secret)
        String resolvedSecret = (newSecret != null && !newSecret.isBlank()) ? newSecret
                : (legacySecret != null && !legacySecret.isBlank()) ? legacySecret
                : DEFAULT_SECRET;
        this.secretKey = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
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
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("type", TOKEN_TYPE_REFRESH)
                .claim("jti", jti)
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

        // Extract jti and check blacklist (prevents refresh token replay)
        String jti = claims.getId();
        if (jti != null && kvStore != null) {
            // If jti already in blacklist, this is a replay attack
            if (!kvStore.put(BLACKLIST_PREFIX + jti, "1", getRemainingSeconds(claims))) {
                log.warn("Refresh token replay detected, jti={}", jti);
                return null;
            }
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

    private long getRemainingSeconds(Claims claims) {
        long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        return remainingMs > 0 ? remainingMs / 1000 : 0;
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
