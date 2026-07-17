package io.mango.job.core.nativeengine;

import io.mango.job.core.service.nativeengine.MangoJobIdempotencyKeys;
import io.mango.job.core.service.nativeengine.MangoJobScheduleIdentity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MangoJobIdempotencyKeysTest {

    private final MangoJobIdempotencyKeys service = new MangoJobIdempotencyKeys();

    @Test
    void scheduledKeyShouldBeStableForSameFireWindow() {
        LocalDateTime fireTime = LocalDateTime.of(2026, 6, 6, 10, 0);

        String first = service.scheduled(new MangoJobScheduleIdentity(10001L, 3, fireTime));
        String second = service.scheduled(new MangoJobScheduleIdentity(10001L, 3, fireTime));

        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    void scheduledKeyShouldChangeWhenScheduleVersionChanges() {
        LocalDateTime fireTime = LocalDateTime.of(2026, 6, 6, 10, 0);

        assertThat(service.scheduled(new MangoJobScheduleIdentity(10001L, 3, fireTime)))
                .isNotEqualTo(service.scheduled(new MangoJobScheduleIdentity(10001L, 4, fireTime)));
    }

    @Test
    void manualKeyShouldRequireBatchNo() {
        assertThatThrownBy(() -> service.manual(10001L, " "))
                .hasMessageContaining("触发批次号不能为空");
    }

    @Test
    void apiAndManualKeysShouldUseDifferentNamespaces() {
        assertThat(service.manual(10001L, "batch-1"))
                .isNotEqualTo(service.api(10001L, "batch-1"));
    }
}
