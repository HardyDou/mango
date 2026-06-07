package io.mango.job.core.service.nativeengine;

import io.mango.common.result.Require;
import io.mango.job.core.entity.MangoJobAttemptEntity;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Job 执行租约服务。
 */
public class MangoJobLeaseService {

    private static final int TOKEN_BYTES = 16;

    private final Clock clock;

    private final SecureRandom secureRandom;

    public MangoJobLeaseService() {
        this(Clock.systemDefaultZone(), new SecureRandom());
    }

    public MangoJobLeaseService(Clock clock, SecureRandom secureRandom) {
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    public String grant(MangoJobAttemptEntity attempt, String leaseOwner, long leaseSeconds) {
        Require.notNull(attempt, "执行尝试不能为空");
        Require.notBlank(leaseOwner, "租约持有者不能为空");
        Require.isTrue(leaseSeconds > 0, "租约秒数必须大于 0");
        String token = newToken();
        attempt.setLeaseOwner(leaseOwner.trim());
        attempt.setLeaseUntil(now().plusSeconds(leaseSeconds));
        attempt.setFencingToken(token);
        return token;
    }

    public void requireToken(MangoJobAttemptEntity attempt, String fencingToken) {
        Require.notNull(attempt, "执行尝试不能为空");
        Require.notBlank(fencingToken, "fencing token 不能为空");
        Require.notBlank(attempt.getFencingToken(), "执行尝试尚未授予 fencing token");
        Require.isTrue(attempt.getFencingToken().equals(fencingToken), "fencing token 已过期");
    }

    public boolean expired(MangoJobAttemptEntity attempt) {
        Require.notNull(attempt, "执行尝试不能为空");
        return attempt.getLeaseUntil() != null && attempt.getLeaseUntil().isBefore(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
