package cn.keking.service;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class ConversionTaskLeaseTest {
    @Test
    void 本地租约释放后同一任务可以再次获取() throws Exception {
        LocalConversionTaskLease lease = new LocalConversionTaskLease();
        assertThat(lease.tryAcquire("k", Duration.ofSeconds(1))).isTrue();
        assertThat(lease.tryAcquire("k", Duration.ofSeconds(1))).isFalse();
        lease.release("k");
        assertThat(lease.tryAcquire("k", Duration.ofSeconds(1))).isTrue();
    }
    @Test
    void 本地租约过期后可以恢复任务() throws Exception {
        LocalConversionTaskLease lease = new LocalConversionTaskLease();
        assertThat(lease.tryAcquire("k", Duration.ofMillis(1))).isTrue();
        Thread.sleep(10);
        assertThat(lease.tryAcquire("k", Duration.ofSeconds(1))).isTrue();
    }
}
