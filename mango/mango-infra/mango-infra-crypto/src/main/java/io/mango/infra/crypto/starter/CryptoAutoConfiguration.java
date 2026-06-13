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
 * 基础密码能力自动配置。
 * <p>
 * 默认只装配当前已验证的 SM4 加解密、SM3 哈希和 SM2 签名能力。
 * AES、RSA、HMAC 等基础实现保留为显式使用的工具类，不默认声明为生产级完整能力。
 */
@Configuration
@ConditionalOnProperty(prefix = "mango.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoAutoConfiguration {

    /**
     * SM4 对称加解密服务。
     */
    @Bean
    @ConditionalOnMissingBean(ICryptoService.class)
    public ICryptoService sm4CryptoService(CryptoProperties properties) {
        return new Sm4CryptoService(properties);
    }

    /**
     * SM3 哈希服务。
     */
    @Bean
    @ConditionalOnMissingBean(Sm3CryptoService.class)
    public Sm3CryptoService sm3CryptoService() {
        return new Sm3CryptoService();
    }

    /**
     * SM2 签名验签服务。
     */
    @Bean
    @ConditionalOnMissingBean(ISignService.class)
    @ConditionalOnProperty(prefix = "mango.crypto.sm2", name = "private-key")
    public ISignService sm2SignService(CryptoProperties properties) {
        return new Sm2SignService(properties);
    }
}
