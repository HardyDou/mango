package io.mango.job.starter;

import io.mango.job.starter.probe.MangoJobRuntimeProbeHandler;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

class JobAutoConfigurationTest {

    @Test
    void shouldEnableProbeHandlerByDefault() throws NoSuchMethodException {
        Method method = JobAutoConfiguration.class.getDeclaredMethod("mangoJobRuntimeProbeHandler");

        ConditionalOnProperty condition = method.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition.prefix()).isEqualTo("mango.job.probe");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
    }
}
