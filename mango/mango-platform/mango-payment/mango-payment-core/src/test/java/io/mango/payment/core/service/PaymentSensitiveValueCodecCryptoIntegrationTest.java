package io.mango.payment.core.service;

import io.mango.infra.crypto.impl.sm.Sm4CryptoService;
import io.mango.infra.crypto.starter.CryptoProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Consumer contract test using the real Crypto implementation rather than a mocked port. */
class PaymentSensitiveValueCodecCryptoIntegrationTest {

    private static final String PUBLIC_TEST_KEY = "0123456789abcdef0123456789abcdef";

    @Test
    void paymentSensitiveValue_roundTripsWithRealSm4AndNeverStoresPlaintext() {
        PaymentSensitiveValueCodec codec = new PaymentSensitiveValueCodec(realSm4());
        String plaintext = "public-payment-test-secret";

        String storedValue = codec.encrypt(plaintext);

        assertThat(storedValue).startsWith("enc:").doesNotContain(plaintext);
        assertThat(codec.decrypt(storedValue)).isEqualTo(plaintext);
        assertThat(codec.encrypt(storedValue)).isEqualTo(storedValue);
    }

    private Sm4CryptoService realSm4() {
        CryptoProperties properties = new CryptoProperties();
        CryptoProperties.Sm4Config sm4 = new CryptoProperties.Sm4Config();
        sm4.setSecretKey(PUBLIC_TEST_KEY);
        sm4.setMode("CBC");
        sm4.setPadding("PKCS5Padding");
        properties.setSm4(sm4);
        return new Sm4CryptoService(properties);
    }
}
