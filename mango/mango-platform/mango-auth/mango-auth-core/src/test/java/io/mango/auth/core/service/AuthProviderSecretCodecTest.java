package io.mango.auth.core.service;

import io.mango.common.exception.BizException;
import io.mango.infra.crypto.impl.ICryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthProviderSecretCodecTest {

    @Test
    void encryptsWithMarkerAndDecryptsOnlyMarkedCiphertext() {
        ICryptoService cryptoService = mock(ICryptoService.class);
        when(cryptoService.encrypt("provider-secret")).thenReturn("ciphertext");
        when(cryptoService.decrypt("ciphertext")).thenReturn("provider-secret");
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("cryptoService", cryptoService);
        AuthProviderSecretCodec codec = new AuthProviderSecretCodec(beans.getBeanProvider(ICryptoService.class));

        String encrypted = codec.encrypt("  provider-secret  ");

        assertThat(encrypted).isEqualTo("enc:ciphertext").doesNotContain("provider-secret");
        assertThat(codec.decrypt(encrypted)).isEqualTo("provider-secret");
        verify(cryptoService).encrypt("provider-secret");
        verify(cryptoService).decrypt("ciphertext");
        assertThatThrownBy(() -> codec.decrypt("provider-secret"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void failsClosedWhenCryptoCapabilityIsUnavailable() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        AuthProviderSecretCodec codec = new AuthProviderSecretCodec(beans.getBeanProvider(ICryptoService.class));

        assertThatThrownBy(() -> codec.encrypt("secret"))
                .isInstanceOf(BizException.class);
    }
}
