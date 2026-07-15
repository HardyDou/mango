package io.mango.authorization.starter;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.api.AuthorityContributor;
import io.mango.authorization.api.IAuthorizationProvider;
import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.api.vo.AuthorizationSnapshotVO;
import io.mango.authorization.api.vo.SecurityContextVO;
import io.mango.authorization.starter.autoconfigure.SecurityAutoConfiguration;
import io.mango.authorization.support.autoconfigure.sensitive.AuthorizationSensitiveRawAccessProvider;
import io.mango.infra.sensitive.api.ISensitiveRawAccessProvider;
import io.mango.infra.sensitive.api.annotation.Sensitive;
import io.mango.infra.sensitive.api.enums.SensitiveType;
import io.mango.infra.sensitive.starter.SensitiveAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveAuthorizationIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    SensitiveAutoConfiguration.class));

    @Test
    void authorizedUser_realAuthorizationProviderAndJacksonModule_returnRawValue() {
        contextRunner.withUserConfiguration(RawAuthorityConfiguration.class)
                .run(context -> {
                    assertThat(context.getBean(ISensitiveRawAccessProvider.class))
                            .isInstanceOf(AuthorizationSensitiveRawAccessProvider.class);
                    ObjectMapper objectMapper = new ObjectMapper()
                            .registerModule(context.getBean(Module.class));

                    String json = write(objectMapper, new AccountView("17612345678"));

                    assertThat(json).contains("17612345678");
                });
    }

    @Test
    void unauthorizedUser_realAuthorizationProviderAndJacksonModule_maskValueAndPreserveDeserialization() {
        contextRunner.withUserConfiguration(MaskedAuthorityConfiguration.class)
                .run(context -> {
                    ObjectMapper objectMapper = new ObjectMapper()
                            .registerModule(context.getBean(Module.class));
                    MutableAccountView value = read(objectMapper, "{\"mobile\":\"17612345678\"}");

                    String json = write(objectMapper, value);

                    assertThat(value.mobile).isEqualTo("17612345678");
                    assertThat(json).contains("176****5678").doesNotContain("17612345678");
                });
    }

    private String write(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("serialization failed", exception);
        }
    }

    private MutableAccountView read(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, MutableAccountView.class);
        } catch (Exception exception) {
            throw new IllegalStateException("deserialization failed", exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RawAuthorityConfiguration extends BaseAuthorizationConfiguration {

        @Bean
        AuthorityContributor rawAuthorityContributor() {
            return query -> AuthorizationSnapshotVO.of(List.of(), List.of(), List.of("no_mask"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MaskedAuthorityConfiguration extends BaseAuthorizationConfiguration {

        @Bean
        AuthorityContributor maskedAuthorityContributor() {
            return query -> AuthorizationSnapshotVO.empty();
        }
    }

    static class BaseAuthorizationConfiguration {

        @Bean
        ISecurityContextProvider securityContextProvider() {
            return () -> new SecurityContextVO(
                    1L, 1001L, "1", true, "admin", "INTERNAL", "INTERNAL_USER",
                    "INTERNAL_ORG", 1L, "internal-admin");
        }

        @Bean
        IAuthorizationProvider authorizationProvider(List<AuthorityContributor> contributors) {
            return new DefaultAuthorizationProvider(contributors);
        }
    }

    record AccountView(@Sensitive(type = SensitiveType.MOBILE_PHONE) String mobile) {
    }

    static class MutableAccountView {

        @Sensitive(type = SensitiveType.MOBILE_PHONE)
        public String mobile;
    }
}
