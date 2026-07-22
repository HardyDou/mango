package io.mango.file.core.service.remote;

import io.mango.common.exception.BizException;
import io.mango.file.api.enums.FileCode;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteImageAddressPolicyTest {

    @Test
    void validate_acceptsPublicDnsAddressAndNormalizesPath() throws Exception {
        RemoteImageAddressPolicy policy = new RemoteImageAddressPolicy(
                host -> new InetAddress[]{InetAddress.getByAddress(host, new byte[]{8, 8, 8, 8})},
                Set.of(80, 443));

        RemoteImageTarget target = policy.validate("https://Example.com/assets/image.png");

        assertThat(target.uri().toString()).isEqualTo("https://example.com/assets/image.png");
        assertThat(target.addresses()).hasSize(1);
    }

    @Test
    void validate_rejectsLocalhostAndIpLiteral() {
        RemoteImageAddressPolicy policy = new RemoteImageAddressPolicy(
                host -> new InetAddress[]{InetAddress.getLoopbackAddress()},
                Set.of(80, 443));

        assertThatThrownBy(() -> policy.validate("http://localhost/image.png"))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getCode()).isEqualTo(FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN.getCode()));
        assertThatThrownBy(() -> policy.validate("http://127.0.0.1/image.png"))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getCode()).isEqualTo(FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN.getCode()));
    }

    @Test
    void validate_rejectsMixedDnsResultsWhenAnyAddressIsForbidden() throws Exception {
        RemoteImageAddressPolicy policy = new RemoteImageAddressPolicy(
                host -> new InetAddress[]{
                        InetAddress.getByAddress(host, new byte[]{8, 8, 8, 8}),
                        InetAddress.getLoopbackAddress(),
                },
                Set.of(80, 443));

        assertThatThrownBy(() -> policy.validate("https://mixed.example/image.png"))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getCode()).isEqualTo(FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN.getCode()));
    }
}
