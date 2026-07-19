package io.mango.payment.starter.remote;

import feign.QueryMapEncoder;
import feign.querymap.BeanQueryMapEncoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * Registers the Payment remote API adapters.
 */
@AutoConfiguration
@EnableFeignClients(basePackageClasses = PaymentApplicationFeignClient.class)
public class PaymentRemoteAutoConfiguration {

    /**
     * Encodes inherited Payment query properties through JavaBean accessors.
     *
     * @return the Payment query-map encoder
     */
    @Bean
    @ConditionalOnMissingBean
    public QueryMapEncoder paymentQueryMapEncoder() {
        return new BeanQueryMapEncoder();
    }
}
