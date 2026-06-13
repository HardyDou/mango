package io.mango.infra.crypto.starter;

import io.mango.infra.crypto.impl.sm.Sm4CryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class CryptoAutoConfigurationTest {

    private static final String LEGACY_SM2_SAMPLE_AS_SM4_KEY = "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgA=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CryptoPropertiesBindingConfiguration.class);

    private final ApplicationContextRunner autoConfigurationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CryptoAutoConfiguration.class));

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
                        "mango.crypto.sm4-key=" + LEGACY_SM2_SAMPLE_AS_SM4_KEY,
                        "mango.crypto.sm4.secret-key=0123456789abcdef0123456789abcdef"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    CryptoProperties properties = ctx.getBean(CryptoProperties.class);
                    assertThat(properties.getSm4().getSecretKey()).isEqualTo("0123456789abcdef0123456789abcdef");
                });
    }

    @Test
    void legacySm2SampleAsSm4Key_shouldFailWithMigrationMessage() {
        contextRunner
                .withPropertyValues("mango.crypto.sm4-key=" + LEGACY_SM2_SAMPLE_AS_SM4_KEY)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(rootCauseMessage(ctx.getStartupFailure()))
                            .contains("历史配置 mango.crypto.sm4-key 使用了 SM2 示例值")
                            .contains("mango.crypto.sm4.secret-key")
                            .contains("00112233445566778899aabbccddeeff");
                });
    }

    @Test
    void autoConfigurationWithLegacySm2SampleAsSm4Key_shouldFailWithMigrationMessage() {
        autoConfigurationContextRunner
                .withPropertyValues(
                        "mango.crypto.sm4-key=" + LEGACY_SM2_SAMPLE_AS_SM4_KEY,
                        "mango.crypto.sm2.private-key=unused",
                        "mango.crypto.sm2.public-key=unused"
                )
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(rootCauseMessage(ctx.getStartupFailure()))
                            .contains("历史配置 mango.crypto.sm4-key 使用了 SM2 示例值")
                            .contains("mango.crypto.sm4.secret-key");
                });
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    @Configuration
    @EnableConfigurationProperties(CryptoProperties.class)
    static class CryptoPropertiesBindingConfiguration {
    }
}
