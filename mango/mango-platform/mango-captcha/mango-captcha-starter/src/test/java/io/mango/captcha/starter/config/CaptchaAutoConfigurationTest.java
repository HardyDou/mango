package io.mango.captcha.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.captcha.api.spi.SmsProvider;
import io.mango.captcha.starter.provider.AliyunSmsProvider;
import io.mango.captcha.starter.provider.DefaultSmsProvider;
import io.mango.captcha.starter.provider.TencentSmsProvider;
import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 短信供应商条件装配测试。
 *
 * <p>锁定 {@link CaptchaAutoConfiguration} 中 {@code SmsProvider} 的条件装配行为：
 * {@code mango.captcha.sms.provider} 决定注册哪个内置供应商，用户自定义 Bean 始终优先；
 * 取值不匹配任何内置供应商时不注册 {@code SmsProvider}（发送链路因无供应商返回 2410，
 * 属于显式失败而非静默成功）。</p>
 */
class CaptchaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
                    CaptchaAutoConfiguration.class))
            .withBean(ObjectMapper.class)
            .withBean(IKvStore.class, () -> mock(IKvStore.class));

    @Test
    void withoutProviderProperty_registersOnlyDefaultProvider() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SmsProvider.class);
            assertThat(context.getBean(SmsProvider.class)).isInstanceOf(DefaultSmsProvider.class);
        });
    }

    @Test
    void withProviderDefault_registersOnlyDefaultProvider() {
        runner.withPropertyValues("mango.captcha.sms.provider=default")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SmsProvider.class);
                    assertThat(context.getBean(SmsProvider.class)).isInstanceOf(DefaultSmsProvider.class);
                });
    }

    @Test
    void withProviderAliyun_registersOnlyAliyunProvider() {
        runner.withPropertyValues("mango.captcha.sms.provider=aliyun")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SmsProvider.class);
                    assertThat(context.getBean(SmsProvider.class)).isInstanceOf(AliyunSmsProvider.class);
                    assertThat(context.getBean(SmsProvider.class).getName()).isEqualTo("aliyun");
                });
    }

    @Test
    void withProviderTencent_registersOnlyTencentProvider() {
        runner.withPropertyValues("mango.captcha.sms.provider=tencent")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SmsProvider.class);
                    assertThat(context.getBean(SmsProvider.class)).isInstanceOf(TencentSmsProvider.class);
                    assertThat(context.getBean(SmsProvider.class).getName()).isEqualTo("tencent");
                });
    }

    /**
     * 未知取值不注册任何内置供应商：发送链路因无 {@code SmsProvider} 返回 2410。
     */
    @Test
    void withUnknownProvider_registersNoSmsProvider() {
        runner.withPropertyValues("mango.captcha.sms.provider=unknown")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SmsProvider.class);
                });
    }

    @Test
    void withUserDefinedProvider_userBeanWinsOverBuiltIn() {
        runner.withUserConfiguration(UserSmsProviderConfiguration.class)
                .withPropertyValues("mango.captcha.sms.provider=aliyun")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SmsProvider.class);
                    assertThat(context.getBean(SmsProvider.class)).isInstanceOf(CustomSmsProvider.class);
                });
    }

    @Test
    void withUserDefinedProviderAndNoProviderProperty_userBeanWins() {
        runner.withUserConfiguration(UserSmsProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SmsProvider.class);
                    assertThat(context.getBean(SmsProvider.class)).isInstanceOf(CustomSmsProvider.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class UserSmsProviderConfiguration {

        @Bean
        SmsProvider customSmsProvider() {
            return new CustomSmsProvider();
        }
    }

    static class CustomSmsProvider implements SmsProvider {

        @Override
        public boolean send(String mobile, String templateCode, String... params) {
            return true;
        }

        @Override
        public String getName() {
            return "custom";
        }
    }
}
