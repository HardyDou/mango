package io.mango.notice.channel.wecom;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryWecomAccessTokenProvider implements WecomAccessTokenProvider {

    private static final long REFRESH_AHEAD_SECONDS = 60;

    private final WecomAccessTokenClient client;
    private final Clock clock;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public InMemoryWecomAccessTokenProvider(WecomAccessTokenClient client) {
        this(client, Clock.systemUTC());
    }

    InMemoryWecomAccessTokenProvider(WecomAccessTokenClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public String getAccessToken(String corpId, String corpSecret) {
        if (!StringUtils.hasText(corpId) || !StringUtils.hasText(corpSecret)) {
            throw new IllegalArgumentException("企业微信 CorpId 和 Secret 不能为空");
        }
        String cacheKey = cacheKey(corpId, corpSecret);
        CachedToken cached = cache.get(cacheKey);
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now.plusSeconds(REFRESH_AHEAD_SECONDS))) {
            return cached.value();
        }
        WecomAccessToken token = client.fetch(corpId, corpSecret);
        long ttl = Math.max(token.expiresInSeconds(), REFRESH_AHEAD_SECONDS + 1);
        CachedToken refreshed = new CachedToken(token.value(), now.plusSeconds(ttl));
        cache.put(cacheKey, refreshed);
        return refreshed.value();
    }

    private String cacheKey(String corpId, String corpSecret) {
        return corpId + ":" + secretFingerprint(corpSecret);
    }

    private String secretFingerprint(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return "%02x%02x%02x%02x".formatted(digest[0] & 0xff, digest[1] & 0xff, digest[2] & 0xff, digest[3] & 0xff);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(secret.hashCode());
        }
    }

    private record CachedToken(String value, Instant expiresAt) {
    }
}
