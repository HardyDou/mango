package io.mango.resource.support;

import io.mango.resource.api.enums.ResourceSyncMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceSyncModeTest {

    @Test
    void newValuesRoundTrip() {
        assertThat(ResourceSyncMode.from("SYNC_ALWAYS")).isEqualTo(ResourceSyncMode.SYNC_ALWAYS);
        assertThat(ResourceSyncMode.from("SYNC_ONCE")).isEqualTo(ResourceSyncMode.SYNC_ONCE);
        assertThat(ResourceSyncMode.from("sync-once")).isEqualTo(ResourceSyncMode.SYNC_ONCE);
        assertThat(ResourceSyncMode.SYNC_ALWAYS.value()).isEqualTo("SYNC_ALWAYS");
        assertThat(ResourceSyncMode.SYNC_ONCE.value()).isEqualTo("SYNC_ONCE");
    }

    @Test
    void blankValueDefaultsToAlways() {
        assertThat(ResourceSyncMode.from(null)).isEqualTo(ResourceSyncMode.SYNC_ALWAYS);
        assertThat(ResourceSyncMode.from("  ")).isEqualTo(ResourceSyncMode.SYNC_ALWAYS);
    }

    @Test
    void legacyValuesMapToNewValues() {
        assertThat(ResourceSyncMode.from("AUTO")).isEqualTo(ResourceSyncMode.SYNC_ALWAYS);
        assertThat(ResourceSyncMode.from("INIT_ONLY")).isEqualTo(ResourceSyncMode.SYNC_ONCE);
        assertThat(ResourceSyncMode.from("MANUAL")).isEqualTo(ResourceSyncMode.SYNC_ONCE);
        assertThat(ResourceSyncMode.from("LOCKED")).isEqualTo(ResourceSyncMode.SYNC_ONCE);
    }

    @Test
    void unknownValueIsRejected() {
        assertThatThrownBy(() -> ResourceSyncMode.from("UNKNOWN_MODE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported resource sync mode");
    }
}
