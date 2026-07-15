package io.mango.infra.iplocation.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IpAddressClassifierTest {

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.0.0.1", "100.64.0.1", "169.254.1.1", "::1", "fd00::1"})
    void shouldRecognizePrivateAndLocalLiterals(String ip) {
        assertThat(IpAddressClassifier.isInvalid(ip)).isFalse();
        assertThat(IpAddressClassifier.isPrivateOrLocal(ip)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "2001:4860:4860::8888", "::ffff:192.0.2.128"})
    void shouldRecognizePublicLiterals(String ip) {
        assertThat(IpAddressClassifier.isInvalid(ip)).isFalse();
        assertThat(IpAddressClassifier.isPrivateOrLocal(ip)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"localhost", "example.com", "127.0.0", "127.0.0.256", "1.2.3.-1", "2001:db8::zz"})
    void shouldRejectHostnamesAndMalformedAddressesWithoutNameResolution(String ip) {
        assertThat(IpAddressClassifier.isInvalid(ip)).isTrue();
        assertThat(IpAddressClassifier.isPrivateOrLocal(ip)).isFalse();
    }
}
