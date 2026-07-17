package io.mango.job.core.service.nativeengine;

import io.mango.common.result.Require;
import io.mango.job.api.enums.JobCode;
import io.mango.job.core.entity.MangoJobAttemptEntity;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Job 执行租约服务。
 */
public class MangoJobLeaseManager {

    private static final int TOKEN_BYTES = 16;

    private final Clock clock;

    private final SecureRandom secureRandom;

    public MangoJobLeaseManager() {
        this(Clock.systemDefaultZone(), new SecureRandom());
    }

    public MangoJobLeaseManager(Clock clock, SecureRandom secureRandom) {
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    public String grant(MangoJobLeaseGrant grant) {
        Require.notNull(grant, JobCode.JOB_INVALID, "执行租约参数不能为空");
        MangoJobAttemptEntity attempt = grant.attempt();
        String leaseOwner = grant.leaseOwner();
        long leaseSeconds = grant.leaseSeconds();
        Require.notNull(attempt, JobCode.JOB_INVALID, "执行尝试不能为空");
        Require.notBlank(leaseOwner, JobCode.JOB_INVALID, "租约持有者不能为空");
        Require.isTrue(leaseSeconds > 0, JobCode.JOB_INVALID, "租约秒数必须大于 0");
        String token = newToken();
        attempt.setLeaseOwner(leaseOwner.trim());
        attempt.setLeaseUntil(now().plusSeconds(leaseSeconds));
        attempt.setFencingToken(token);
        return token;
    }

    public void requireToken(MangoJobAttemptEntity attempt, String fencingToken) {
        Require.notNull(attempt, JobCode.JOB_INVALID, "执行尝试不能为空");
        Require.notBlank(fencingToken, JobCode.JOB_INVALID, "fencing token 不能为空");
        Require.notBlank(attempt.getFencingToken(), JobCode.JOB_INVALID, "执行尝试尚未授予 fencing token");
        Require.isTrue(attempt.getFencingToken().equals(fencingToken), JobCode.JOB_INVALID, "fencing token 已过期");
    }

    public boolean expired(MangoJobAttemptEntity attempt) {
        Require.notNull(attempt, JobCode.JOB_INVALID, "执行尝试不能为空");
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
