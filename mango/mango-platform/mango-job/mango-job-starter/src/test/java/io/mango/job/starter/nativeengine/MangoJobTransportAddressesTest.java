package io.mango.job.starter.nativeengine;

import io.mango.job.support.nativeengine.MangoJobTransportAddresses;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MangoJobTransportAddressesTest {

    @Test
    void shouldRecognizeEmbeddedWorkerAddresses() {
        assertThat(MangoJobTransportAddresses.isEmbedded("embedded://192.168.1.20:18554")).isTrue();
        assertThat(MangoJobTransportAddresses.isEmbedded("in-memory://host/embedded-123")).isTrue();
        assertThat(MangoJobTransportAddresses.isEmbedded("http://192.168.1.20:18554")).isFalse();
    }
}
