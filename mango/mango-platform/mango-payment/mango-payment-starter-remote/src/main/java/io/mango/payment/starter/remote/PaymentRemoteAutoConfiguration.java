package io.mango.payment.starter.remote;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "io.mango.payment.starter.remote")
public class PaymentRemoteAutoConfiguration {
}
