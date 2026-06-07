package io.mango.job.core.nativeengine;

import io.mango.job.api.enums.JobTransportType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobTransportTypeTest {

    @Test
    void shouldExposeRequiredNativeTransportTypes() {
        assertThat(JobTransportType.values())
                .containsExactly(JobTransportType.IN_MEMORY, JobTransportType.HTTP_INTERNAL);
    }
}
