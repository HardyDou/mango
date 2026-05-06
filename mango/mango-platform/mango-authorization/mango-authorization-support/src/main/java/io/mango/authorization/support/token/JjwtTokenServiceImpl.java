package io.mango.authorization.support.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.mango.infra.kv.api.IKvStore;
import io.mango.authorization.api.ITokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 JJWT 的 {@link ITokenProvider} 实现。
 *
 * @author Mango
 */
@Slf4j
public class JjwtTokenServiceImpl implements ITokenProvider {

    private static final String NEW_SECRET_PROP = "mango.security.jwt.secret";
    private static final String LEGACY_SECRET_PROP = "mango.jwt.secret";
    private static final String BLACKLIST_PREFIX = "jwt:refresh:jti:";
    private static final int MIN_SECRET_BYTES = 32;

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
        // 兼容新配置路径 mango.security.jwt.secret 和历史配置路径 mango.jwt.secret。
        String resolvedSecret = (newSecret != null && !newSecret.isBlank()) ? newSecret
                : (legacySecret != null && !legacySecret.isBlank()) ? legacySecret : null;
        if (resolvedSecret == null || resolvedSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is required. Set " + NEW_SECRET_PROP + " or legacy " + LEGACY_SECRET_PROP + ".");
        }
        if (resolvedSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits. Check " + NEW_SECRET_PROP + ".");
        }
        if (kvStore == null) {
            log.warn("IKvStore bean not found; refresh token replay protection is disabled.");
        }
        this.secretKey = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
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
        return generateRefreshToken(userId, username, Map.of());
    }

    @Override
    public String generateRefreshToken(Long userId, String username, Map<String, Object> extraClaims) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .id(jti)
                .claims(extraClaims)
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
    public String getClaim(String token, String claimName) {
        Claims claims = parseToken(token);
        if (claims == null || claimName == null || claimName.isBlank()) {
            return null;
        }
        Object value = claims.get(claimName);
        return value == null ? null : value.toString();
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

        // 提取 jti 并写入黑名单，防止 refresh token 被重复使用。
        String jti = claims.getId();
        if (jti != null && kvStore != null) {
            // jti 已存在表示同一个 refresh token 被重复提交。
            if (!kvStore.setIfAbsent(BLACKLIST_PREFIX + jti, "1", getRemainingSeconds(claims))) {
                log.warn("Refresh token replay detected, jti={}", jti);
                return null;
            }
        }

        Long userId = getUserId(refreshToken);
        String username = getUsername(refreshToken);
        if (userId == null || username == null) {
            return null;
        }

        Map<String, Object> retainedClaims = retainedIdentityClaims(claims);
        String newAccessToken = generateAccessToken(userId, username, retainedClaims);
        String newRefreshToken = generateRefreshToken(userId, username, retainedClaims);
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    private Map<String, Object> retainedIdentityClaims(Claims claims) {
        java.util.LinkedHashMap<String, Object> retainedClaims = new java.util.LinkedHashMap<>();
        putIfPresent(retainedClaims, "username", claims.get("username"));
        putIfPresent(retainedClaims, "realm", claims.get("realm"));
        putIfPresent(retainedClaims, "actorType", claims.get("actorType"));
        putIfPresent(retainedClaims, "partyType", claims.get("partyType"));
        putIfPresent(retainedClaims, "partyId", claims.get("partyId"));
        putIfPresent(retainedClaims, "appCode", claims.get("appCode"));
        return retainedClaims;
    }

    private void putIfPresent(Map<String, Object> claims, String key, Object value) {
        if (value != null) {
            claims.put(key, value);
        }
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
            log.debug("JWT parse failed: {}", e.getMessage());
            return null;
        }
    }
}
