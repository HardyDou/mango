package io.mango.payment.starter.remote;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRemoteAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class, PaymentRemoteAutoConfiguration.class))
            .withPropertyValues(PaymentRemoteContractFixtures.feignTypes().stream()
                    .map(type -> "spring.cloud.openfeign.client.config."
                            + type.getAnnotation(FeignClient.class).contextId()
                            + ".url=http://localhost")
                    .toArray(String[]::new));

    @Test
    void autoConfiguration_registersEveryPaymentFeignAdapter() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            PaymentRemoteContractFixtures.feignTypes().forEach(type -> assertThat(context.getBeansOfType(type))
                    .as(type.getSimpleName())
                    .hasSize(1));
        });
    }

    @Test
    void consumerCanInjectEveryPaymentApiContract() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            PaymentRemoteContractFixtures.feignTypes().forEach(type -> {
                Class<?> apiType = PaymentRemoteContractFixtures.apiType(type);
                assertThat(context.getBeansOfType(apiType)).as(apiType.getSimpleName()).hasSize(1);
                assertThat(apiType.isInstance(context.getBean(type))).isTrue();
            });
        });
    }
}
