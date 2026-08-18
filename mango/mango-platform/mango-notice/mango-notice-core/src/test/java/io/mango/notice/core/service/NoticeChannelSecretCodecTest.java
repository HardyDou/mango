package io.mango.notice.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mango.common.exception.BizException;
import io.mango.infra.crypto.impl.ICryptoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class NoticeChannelSecretCodecTest {

    @Test
    void encryptsWithoutPersistingPlaintextAndDecrypts() {
        NoticeChannelSecretCodec codec = codec(new TestCryptoService());

        String ciphertext = codec.encrypt("smtp-password-825");

        assertThat(ciphertext).startsWith("enc:").doesNotContain("smtp-password-825");
        assertThat(codec.decrypt(ciphertext)).isEqualTo("smtp-password-825");
    }

    @Test
    void failsClosedWhenCryptoCapabilityIsMissing() {
        NoticeChannelSecretCodec codec = codec(null);

        assertThatThrownBy(() -> codec.encrypt("secret"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("加密能力不可用");
        assertThatThrownBy(() -> codec.decrypt("enc:c2VjcmV0"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("解密能力不可用");
    }

    @Test
    void rejectsMalformedCiphertextWithoutReturningStoredContent() {
        NoticeChannelSecretCodec codec = codec(new TestCryptoService());

        assertThatThrownBy(() -> codec.decrypt("plain-secret"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("密文格式无效")
                .hasMessageNotContaining("plain-secret");
        assertThatThrownBy(() -> codec.decrypt("enc:not-base64"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("解密失败")
                .hasMessageNotContaining("not-base64");
    }

    private static NoticeChannelSecretCodec codec(ICryptoService cryptoService) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        if (cryptoService != null) {
            beanFactory.addBean("cryptoService", cryptoService);
        }
        return new NoticeChannelSecretCodec(beanFactory.getBeanProvider(ICryptoService.class));
    }

    private static final class TestCryptoService implements ICryptoService {
        @Override
        public String encrypt(String plaintext) {
            return Base64.getEncoder()
                    .encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String encrypt(String plaintext, String iv) {
            return encrypt(plaintext);
        }

        @Override
        public String decrypt(String ciphertext) {
            return new String(
                    Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
        }

        @Override
        public String decrypt(String ciphertext, String iv) {
            return decrypt(ciphertext);
        }
    }
}
