package io.mango.authorization.starter.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认安全链装配条件测试。
 */
@DisplayName("Authorization 默认安全链装配条件测试")
class SecurityAutoConfigurationConditionTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class));

    @Test
    @DisplayName("完整管理端关闭默认链时不应创建第二条全请求安全链")
    void defaultChainDisabled_doesNotCreateSecurityFilterChain() {
        contextRunner
                .withPropertyValues("mango.security.default-chain-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SecurityFilterChain.class));
    }
}
