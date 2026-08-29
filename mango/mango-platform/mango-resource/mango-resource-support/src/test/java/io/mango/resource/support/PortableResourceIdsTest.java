package io.mango.resource.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortableResourceIdsTest {

    @Test
    void stable_sameIdentity_returnsSamePositiveId() {
        long first = PortableResourceIds.stable("calendar", "1", "CN_STANDARD");
        long second = PortableResourceIds.stable("calendar", "1", "CN_STANDARD");

        assertThat(first).isPositive().isEqualTo(second);
    }

    @Test
    void stable_fieldBoundaryChanges_returnsDifferentId() {
        long first = PortableResourceIds.stable("calendar_day", "1", "CN", "2026-01-01");
        long second = PortableResourceIds.stable("calendar_day", "1", "CN2026", "-01-01");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void declaredOrStable_nonPositiveDeclaredId_fails() {
        assertThatThrownBy(() -> PortableResourceIds.declaredOrStable(0L, "calendar", "1", "CN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
