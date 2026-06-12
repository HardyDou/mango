package io.mango.infra.crypto.starter;

import io.mango.infra.crypto.impl.sm.Sm4CryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class CryptoAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CryptoPropertiesBindingConfiguration.class);

    @Test
    void legacySm4KeyProperty_shouldBindToNestedSecretKey() {
        contextRunner
                .withPropertyValues(
                        "mango.crypto.sm4-key=00112233445566778899aabbccddeeff",
                        "mango.crypto.sm4-iv=1234567890abcdef1234567890abcdef"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    CryptoProperties properties = ctx.getBean(CryptoProperties.class);
                    assertThat(properties.getSm4().getSecretKey()).isEqualTo("00112233445566778899aabbccddeeff");
                    assertThat(properties.getSm4Key()).isEqualTo("00112233445566778899aabbccddeeff");
                    assertThat(properties.getSm4Iv()).isEqualTo("1234567890abcdef1234567890abcdef");
                    assertThatCode(() -> new Sm4CryptoService(properties)).doesNotThrowAnyException();
                });
    }

    @Test
    void nestedSm4SecretKey_shouldOverrideLegacySm4KeyProperty() {
        contextRunner
                .withPropertyValues(
                        "mango.crypto.sm4-key=00112233445566778899aabbccddeeff",
                        "mango.crypto.sm4.secret-key=0123456789abcdef0123456789abcdef"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    CryptoProperties properties = ctx.getBean(CryptoProperties.class);
                    assertThat(properties.getSm4().getSecretKey()).isEqualTo("0123456789abcdef0123456789abcdef");
                });
    }

    @Configuration
    @EnableConfigurationProperties(CryptoProperties.class)
    static class CryptoPropertiesBindingConfiguration {
    }
}
