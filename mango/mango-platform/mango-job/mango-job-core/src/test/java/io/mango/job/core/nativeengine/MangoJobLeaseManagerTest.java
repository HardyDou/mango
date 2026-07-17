package io.mango.job.core.nativeengine;

import io.mango.job.core.entity.MangoJobAttemptEntity;
import io.mango.job.core.service.nativeengine.MangoJobLeaseManager;
import io.mango.job.core.service.nativeengine.MangoJobLeaseGrant;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MangoJobLeaseManagerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-06T10:00:00Z"),
            ZoneId.of("UTC"));

    private final MangoJobLeaseManager service = new MangoJobLeaseManager(FIXED_CLOCK, new SecureRandom());

    @Test
    void grantShouldSetLeaseOwnerUntilAndToken() {
        MangoJobAttemptEntity attempt = new MangoJobAttemptEntity();

        String token = service.grant(new MangoJobLeaseGrant(attempt, "worker-1", 30));

        assertThat(attempt.getLeaseOwner()).isEqualTo("worker-1");
        assertThat(attempt.getLeaseUntil()).isEqualTo("2026-06-06T10:00:30");
        assertThat(attempt.getFencingToken()).isEqualTo(token).hasSize(32);
    }

    @Test
    void requireTokenShouldRejectStaleToken() {
        MangoJobAttemptEntity attempt = new MangoJobAttemptEntity();
        String token = service.grant(new MangoJobLeaseGrant(attempt, "worker-1", 30));

        assertThatNoException().isThrownBy(() -> service.requireToken(attempt, token));
        assertThatThrownBy(() -> service.requireToken(attempt, "stale"))
                .hasMessageContaining("fencing token 已过期");
    }

    @Test
    void expiredShouldUseLeaseUntil() {
        MangoJobAttemptEntity attempt = new MangoJobAttemptEntity();
        service.grant(new MangoJobLeaseGrant(attempt, "worker-1", 30));
        assertThat(service.expired(attempt)).isFalse();
    }
}
