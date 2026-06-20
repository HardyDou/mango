package io.mango.notice.starter;

import io.mango.authorization.api.ISecurityContextProvider;
import io.mango.authorization.support.autoconfigure.context.SpringSecurityContextProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "io.mango.notice")
@MapperScan("io.mango.notice.core.mapper")
public class NoticeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ISecurityContextProvider.class)
    public ISecurityContextProvider securityContextProvider() {
        return new SpringSecurityContextProvider();
    }
}
