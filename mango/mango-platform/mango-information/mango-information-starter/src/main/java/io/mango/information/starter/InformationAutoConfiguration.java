package io.mango.information.starter;

import io.mango.identity.api.IdentityUserApi;
import io.mango.information.core.mapper.InformationFormMapper;
import io.mango.information.core.service.InformationActorNameProvider;
import io.mango.information.starter.actor.IdentityInformationActorNameProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/** 信息采集自动配置。 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mango.information", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.information.core.mapper")
@ComponentScan({"io.mango.information.core.service", "io.mango.information.starter.controller"})
public class InformationAutoConfiguration {

    /**
     * 平台身份能力存在时装配人员显示名适配；缺失时列表和详情回退展示兜底文案。
     */
    @Bean
    @ConditionalOnClass(IdentityUserApi.class)
    @ConditionalOnMissingBean(InformationActorNameProvider.class)
    public InformationActorNameProvider informationActorNameProvider(ObjectProvider<IdentityUserApi> identityUserApi) {
        return new IdentityInformationActorNameProvider(identityUserApi.getIfAvailable());
    }

    static Class<?> marker() {
        return InformationFormMapper.class;
    }
}
