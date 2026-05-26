package io.mango.notice.channel.wechat.official;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryWechatOfficialAccessTokenProvider implements WechatOfficialAccessTokenProvider {

    private static final long REFRESH_AHEAD_SECONDS = 60;

    private final WechatOfficialAccessTokenClient client;
    private final Clock clock;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public InMemoryWechatOfficialAccessTokenProvider(WechatOfficialAccessTokenClient client) {
        this(client, Clock.systemUTC());
    }

    InMemoryWechatOfficialAccessTokenProvider(WechatOfficialAccessTokenClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public String getAccessToken(String appId, String appSecret) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new IllegalArgumentException("微信公众号 appId 和 appSecret 不能为空");
        }
        CachedToken cached = cache.get(appId);
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now.plusSeconds(REFRESH_AHEAD_SECONDS))) {
            return cached.value();
        }
        WechatOfficialAccessToken token = client.fetch(appId, appSecret);
        long ttl = Math.max(token.expiresInSeconds(), REFRESH_AHEAD_SECONDS + 1);
        CachedToken refreshed = new CachedToken(token.value(), now.plusSeconds(ttl));
        cache.put(appId, refreshed);
        return refreshed.value();
    }

    private record CachedToken(String value, Instant expiresAt) {
    }
}
