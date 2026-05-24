package io.mango.payment.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mango.payment", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.payment.core.mapper")
@ComponentScan({
        "io.mango.payment.core.service",
        "io.mango.payment.channel.sandbox",
        "io.mango.payment.starter.service",
        "io.mango.payment.starter.controller"
})
public class PaymentAutoConfiguration {
}
