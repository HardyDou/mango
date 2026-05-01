package io.mango.authorization.security.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.authorization.api.security.ITokenProvider;
import io.mango.authorization.security.core.impl.JjwtTokenServiceImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Token 能力自动配置。
 * <p>
 * 提供基于 JJWT 的 {@link ITokenProvider} 实现。
 *
 * @author Mango
 */
@AutoConfiguration
public class TokenAutoConfiguration {

    /**
     * 缺少自定义 ITokenProvider 时注册默认 JJWT 实现。
     */
    @Bean
    @ConditionalOnMissingBean(ITokenProvider.class)
    public ITokenProvider tokenProvider(ObjectProvider<IKvStore> kvStoreProvider) {
        return new JjwtTokenServiceImpl(kvStoreProvider.getIfAvailable());
    }
}
