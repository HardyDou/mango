package io.mango.job.core.nativeengine;

import io.mango.job.core.service.nativeengine.MangoJobIdempotencyKeyService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MangoJobIdempotencyKeyServiceTest {

    private final MangoJobIdempotencyKeyService service = new MangoJobIdempotencyKeyService();

    @Test
    void scheduledKeyShouldBeStableForSameFireWindow() {
        LocalDateTime fireTime = LocalDateTime.of(2026, 6, 6, 10, 0);

        String first = service.scheduled(10001L, 3, fireTime);
        String second = service.scheduled(10001L, 3, fireTime);

        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    void scheduledKeyShouldChangeWhenScheduleVersionChanges() {
        LocalDateTime fireTime = LocalDateTime.of(2026, 6, 6, 10, 0);

        assertThat(service.scheduled(10001L, 3, fireTime))
                .isNotEqualTo(service.scheduled(10001L, 4, fireTime));
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
