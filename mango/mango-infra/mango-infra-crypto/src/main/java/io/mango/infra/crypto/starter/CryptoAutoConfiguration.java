package io.mango.infra.crypto.starter;

import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.crypto.impl.ISignService;
import io.mango.infra.crypto.impl.sm.Sm2SignService;
import io.mango.infra.crypto.impl.sm.Sm4CryptoService;
import io.mango.infra.crypto.impl.sm.Sm3CryptoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crypto auto-configuration.
 * Enables SM2/SM3/SM4 cryptographic services based on configuration.
 */
@Configuration
@ConditionalOnProperty(prefix = "mango.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoAutoConfiguration {

    /**
     * SM4 symmetric encryption/decryption service.
     */
    @Bean
    @ConditionalOnMissingBean(ICryptoService.class)
    public ICryptoService sm4CryptoService(CryptoProperties properties) {
        return new Sm4CryptoService(properties);
    }

    /**
     * SM3 hash service.
     */
    @Bean
    @ConditionalOnMissingBean(Sm3CryptoService.class)
    public Sm3CryptoService sm3CryptoService() {
        return new Sm3CryptoService();
    }

    /**
     * SM2 signature service.
     */
    @Bean
    @ConditionalOnMissingBean(ISignService.class)
    public ISignService sm2SignService(CryptoProperties properties) {
        return new Sm2SignService(properties);
    }
}
